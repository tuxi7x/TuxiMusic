package io.tuxi.repositories;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.tuxi.managers.GuildMusicManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class MusicRepositoryImpl implements MusicRepository {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public MusicRepositoryImpl(AudioPlayerManager playerManager, Map<Long, GuildMusicManager> musicManagers) {
        this.playerManager = playerManager;
        this.musicManagers = musicManagers;
    }


    @Override
    public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    @Override
    public void loadAndPlay(SlashCommandEvent event, String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getTextChannel().getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                event.reply("Adding to queue " + track.getInfo().title).queue();

                play(event, musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();
                List<AudioTrack> playlistTracks = playlist.getTracks();
                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                    play(event, musicManager, firstTrack);
                    playlistTracks.remove(firstTrack);
                }
                for (AudioTrack track : playlistTracks) {
                    if (track != null) {
                        play(event, musicManager, track);
                    }
                }
                event.reply("Playlist named " + playlist.getName() + " of " + (playlistTracks.size() + 1) + " tracks added to queue! ").queue();
            }

            @Override
            public void noMatches() {
                event.reply("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.reply("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    @Override
    public void play(SlashCommandEvent event, GuildMusicManager musicManager, AudioTrack track) {
        Guild guild = event.getTextChannel().getGuild();
        // The warning is suppressed, because the null check is done before using the variable
        @SuppressWarnings("ConstantConditions")
        VoiceChannel senderVoiceChannel = event.getMember().getVoiceState().getChannel();
        if (senderVoiceChannel != null) {
            connectToVoiceChannel(senderVoiceChannel, guild.getAudioManager());
        } else {
            connectToFirstVoiceChannel(guild.getAudioManager());
        }
        musicManager.scheduler.queue(track);
    }

    @Override
    public void skipTrack(SlashCommandEvent event) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getTextChannel().getGuild());
        List<AudioTrack> tracks = musicManager.scheduler.getTracks();
        if (tracks.size() == 0) {
            event.reply("Skipped the track, and the queue is empty!").queue();
            musicManager.scheduler.nextTrack();
            return;
        }
        musicManager.scheduler.nextTrack();
        event.reply("Skipped to next track.").queue();
    }

    @Override
    public void sendQueue(SlashCommandEvent event) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getTextChannel().getGuild());
        List<AudioTrack> tracks = musicManager.scheduler.getTracks();

        if (tracks.size() == 0) {
            event.reply("The queue is empty!").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("TuxiMusic");
        embed.setTitle("Songs in queue", null);
        embed.setColor(Color.yellow);

        for (int i = 0; i < tracks.size(); i++) {
            AudioTrack track = tracks.get(i);
            String title = track.getInfo().title;
            long duration = track.getDuration();

            String line = title + " (" + millisecondsToTime(duration) + ")";
            embed.addField(String.valueOf(i + 1), line, false);
        }
        event.replyEmbeds(embed.build()).queue();
    }

    @Override
    public void sendCurrent(SlashCommandEvent event) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getTextChannel().getGuild());
        AudioTrack track = musicManager.player.getPlayingTrack();
        if (track == null) {
            event.reply("There is no song playing right now!").queue();
            return;
        }
        String title = track.getInfo().title;
        long duration = track.getDuration();
        long position = track.getPosition();


        event.reply("Now playing: " + title + " (" + millisecondsToTime(position) + "/" + millisecondsToTime(duration) + ")").queue();
    }

    @Override
    public void removeTrackFromQueue(SlashCommandEvent event, int positionOfTrack) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getTextChannel().getGuild());
        if (positionOfTrack < 1) {
            event.reply("The removed song's position must be a positive number!").queue();
            return;
        }
        AudioTrack removedSong = musicManager.scheduler.removeTrackInQueue(positionOfTrack);
        if (removedSong == null) {
            event.reply("Couldn't remove song from queue with the given position!").queue();
            return;
        }

        event.reply("Successfully removed song from queue: " + removedSong.getInfo().title).queue();
    }

    @Override
    public void connectToVoiceChannel(@Nullable VoiceChannel voiceChannel, AudioManager audioManager) {
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
        }
    }


    @Override
    public void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected()) {
            for (VoiceChannel vc : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(vc);
                break;
            }
        }
    }


    private String millisecondsToTime(long milliseconds) {
        long minutes = (milliseconds / 1000) / 60;
        long seconds = (milliseconds / 1000) % 60;
        String secondsStr = Long.toString(seconds);
        String secs;
        if (secondsStr.length() >= 2) {
            secs = secondsStr.substring(0, 2);
        } else {
            secs = "0" + secondsStr;
        }

        return minutes + ":" + secs;
    }


}
