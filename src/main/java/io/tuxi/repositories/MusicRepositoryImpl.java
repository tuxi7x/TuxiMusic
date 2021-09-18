package io.tuxi.repositories;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.tuxi.managers.GuildMusicManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Duration;
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

                play(event.getTextChannel().getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                event.reply("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(event.getTextChannel().getGuild(), musicManager, firstTrack);
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
    public void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        MusicRepository.connectToFirstVoiceChannel(guild.getAudioManager());

        musicManager.scheduler.queue(track);
    }

    @Override
    public void skipTrack(SlashCommandEvent event) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getTextChannel().getGuild());
        List<AudioTrack> tracks = musicManager.scheduler.getTracks();
        if (tracks.size() == 0) {
            event.reply("The queue is empty!").queue();
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

        for (int i = 0; i<tracks.size(); i++) {
            AudioTrack track = tracks.get(i);
            String title = track.getInfo().title;
            long duration = track.getDuration();
            Duration dur = Duration.ofMillis(duration);
            String durStr = String.format("%02d:%02d",
                    dur.toMinutesPart(), dur.toSecondsPart());

            String line = title + " (" + durStr + ")";
            embed.addField(String.valueOf(i+1), line, false);
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
        Duration dur = Duration.ofMillis(duration);
        String durStr = String.format("%02d:%02d",
                dur.toMinutesPart(), dur.toSecondsPart());
        Duration pos = Duration.ofMillis(position);
        String posStr = String.format("%02d:%02d",
                pos.toMinutesPart(), pos.toSecondsPart());

        event.reply("Now playing: " + title + " (" + posStr + "/" + durStr + ")").queue();
    }
}
