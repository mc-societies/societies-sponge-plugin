package org.societies.sponge;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import net.catharos.lib.core.command.Commands;
import net.catharos.lib.core.command.sender.Sender;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.shank.logging.LoggingModule;
import org.shank.service.ServiceController;
import org.shank.service.ServiceModule;
import org.shank.service.lifecycle.Lifecycle;
import org.societies.SocietiesModule;
import org.societies.groups.member.Member;
import org.societies.groups.member.MemberProvider;
import org.societies.util.LoggerWrapper;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.message.CommandEvent;
import org.spongepowered.api.event.state.ServerStartedEvent;
import org.spongepowered.api.event.state.ServerStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.event.Subscribe;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Futures.addCallback;

/**
 * Represents a SpongePlugin
 */
@Plugin(id = "societies", name = "societies", version = "0.1")
public class SpongePlugin {
    private Injector injector;

    private Commands<Sender> commands;
    private MemberProvider memberProvider;
    private ServiceController serviceController;
    private Sender systemSender;
    private Logger logger;
    private Game game;

    @Subscribe
    public void onStart(ServerStartedEvent event) {
        game = event.getGame();

        logger = new LoggerWrapper(java.util.logging.Logger.getLogger("sponge-societies"));

        File dir = new File(".");

        logger.info("Reloading AK-47... Please wait patiently!");

        injector = Guice.createInjector(
                new ServiceModule(),
                new LoggingModule(logger),
                new SocietiesModule(dir, logger),
                new BukkitModule(plugin.getServer(), plugin, this, economy)
        );

        logger.info("Well done.");

        serviceController = injector.getInstance(ServiceController.class);

        serviceController.invoke(Lifecycle.INITIALISING);


        commands = injector.getInstance(Key.get(new TypeLiteral<Commands<Sender>>() {}));
        memberProvider = injector.getInstance(Key.get(new TypeLiteral<MemberProvider>() {}));
        systemSender = injector.getInstance(Key.get(Sender.class, Names.named("system-sender")));


        serviceController.invoke(Lifecycle.STARTING);
    }

    @Subscribe
    public void onStop(ServerStoppingEvent event) {
        if (injector == null) {
            return;
        }

        ListeningExecutorService service = injector.getInstance(ListeningExecutorService.class);

        service.shutdown();

        try {

            service.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Nobody fucking cares!
            logger.catching(e);
        }

        serviceController.invoke(Lifecycle.STOPPING);

        logger.info("Engines and weapons unloaded and locked!");
    }

    @Subscribe
    public void onCommand(CommandEvent event) {
        CommandSource sender = event.getSource();
        final String command = event.getCommand() + " " + event.getArguments();

        if (injector == null) {
            sender.sendMessage("Societies failed to start somehow, sorry :/ Fuck the dev!!");
            return;
        }

        if (sender instanceof Player) {
            ListenableFuture<Member> future = memberProvider.getMember(((Player) sender).getUniqueId());

            addCallback(future, new FutureCallback<Member>() {
                @Override
                public void onSuccess(@Nullable Member result) {
                    commands.execute(result, command);
                }

                @Override
                public void onFailure(@NotNull Throwable t) {
                    logger.catching(t);
                }
            });


        } else {
            commands.execute(systemSender, command);
        }
    }


    @Override
    public void reload() {
        onStart();
        onStop();
    }

}
