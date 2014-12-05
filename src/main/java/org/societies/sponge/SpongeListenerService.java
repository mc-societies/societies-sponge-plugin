package org.societies.sponge;

import org.shank.service.AbstractService;
import org.shank.service.lifecycle.LifecycleContext;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.event.EventManager;

/**
 * Represents a SpongeListenerService
 */
public class SpongeListenerService extends AbstractService {

    private final Object plugin;
    private final Game game;

    public SpongeListenerService(Object plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @Override
    public void start(LifecycleContext context) throws Exception {
        EventManager pluginManager = game.getEventManager();
        pluginManager.register(plugin, context.get(ChatListener.class));
        pluginManager.register(plugin, context.get(DamageListener.class));
        pluginManager.register(plugin, context.get(SpawnListener.class));
        pluginManager.register(plugin, context.get(JoinListener.class));
    }
}
