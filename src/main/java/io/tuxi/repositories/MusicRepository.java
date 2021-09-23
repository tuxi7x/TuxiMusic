package io.tuxi.repositories;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.tuxi.managers.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * Repository containing the required commands for operating a music bot
 */
public interface MusicRepository {


    GuildMusicManager getGuildAudioPlayer(Guild guild);
    void loadAndPlay(final SlashCommandEvent event, final String trackUrl);
    void play(SlashCommandEvent event, GuildMusicManager musicManager, AudioTrack track);
    void skipTrack(SlashCommandEvent event);
    void sendQueue(SlashCommandEvent event);
    void sendCurrent(SlashCommandEvent event);
    void removeTrackFromQueue(SlashCommandEvent event, int positionOfTrack);

    void connectToVoiceChannel(VoiceChannel voiceChannel, AudioManager audioManager);
    void connectToFirstVoiceChannel(AudioManager audioManager);


}
