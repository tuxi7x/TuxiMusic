package io.tuxi.listeners;

import io.tuxi.repositories.MusicRepository;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CommandListener extends ListenerAdapter {

    private final MusicRepository musicRepository;

    public CommandListener(MusicRepository musicRepository) {
        this.musicRepository = musicRepository;
    }


    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getName().equals("play")) {
            String url = Objects.requireNonNull(event.getOption("url")).getAsString();
            musicRepository.loadAndPlay(event, url);
        } else if (event.getName().equals("skip")) {
            musicRepository.skipTrack(event);
        } else if (event.getName().equals("queue")) {
            musicRepository.sendQueue(event);
        } else if (event.getName().equals("current")) {
            musicRepository.sendCurrent(event);
        } else if (event.getName().equals("remove")) {
            long positionOfTrack = Objects.requireNonNull(event.getOption("position")).getAsLong();
            musicRepository.removeTrackFromQueue(event,(int)positionOfTrack);
        }


        super.onSlashCommand(event);
    }
}
