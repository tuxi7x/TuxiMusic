package io.tuxi;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import io.tuxi.listeners.CommandListener;
import io.tuxi.managers.GuildMusicManager;
import io.tuxi.repositories.MusicRepositoryImpl;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.entities.ActivityImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("You have to provide a token as first argument!");
            System.exit(1);
        }


        Map<Long, GuildMusicManager> musicManagers = new HashMap<>();

        AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);

        Activity botActivity = new Activity() {
            @Override
            public boolean isRich() {
                return false;
            }

            @Nullable
            @Override
            public RichPresence asRichPresence() {
                return null;
            }

            @NotNull
            @Override
            public String getName() {
                return "TuxiMusic is live!";
            }

            @Override
            @Nullable
            public String getUrl() {
                return null;
            }

            @NotNull
            @Override
            public ActivityType getType() {
                return ActivityType.LISTENING;
            }

            @Nullable
            @Override
            public Timestamps getTimestamps() {
                return null;
            }

            @Nullable
            @Override
            public Emoji getEmoji() {
                return null;
            }
        };

        JDA jda = JDABuilder.create(args[0], GUILD_MESSAGES, GUILD_VOICE_STATES)
                .addEventListeners(new CommandListener(new MusicRepositoryImpl(audioPlayerManager, musicManagers)))
                .setActivity(botActivity)
                .build();

        jda.upsertCommand("play", "Play a music").addOption(OptionType.STRING, "url", "The url of the music", true).queue();
        jda.upsertCommand("skip", "Skip a music").queue();
        jda.upsertCommand("current", "Get current music").queue();
        jda.upsertCommand("queue", "Get songs in queue").queue();
    }
}
