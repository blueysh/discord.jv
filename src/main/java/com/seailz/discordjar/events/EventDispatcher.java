package com.seailz.discordjar.events;

import com.seailz.discordjar.DiscordJar;
import com.seailz.discordjar.events.annotation.EventMethod;
import com.seailz.discordjar.events.model.Event;
import com.seailz.discordjar.events.model.interaction.CustomIdable;
import com.seailz.discordjar.events.model.interaction.InteractionEvent;
import com.seailz.discordjar.events.model.message.MessageCreateEvent;
import com.seailz.discordjar.utils.annotation.RequireCustomId;
import com.seailz.discordjar.utils.rest.DiscordRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * This class is used to dispatch events to the correct listeners.
 * This class is an internal class and should not be used by the end user.
 *
 * @author Seailz
 * @since 1.0
 */
public class EventDispatcher {

    private final HashMap<DiscordListener, Method> listeners;

    public EventDispatcher(DiscordJar bot) {
        listeners = new HashMap<>();
    }

    /**
     * Registers a listener to the dispatcher.
     * When an event is fired, the dispatcher will call the listener method that is marked with the {@link EventMethod} annotation.
     *
     * @param listeners The listeners to register
     * @since 1.0
     */
    public void addListener(DiscordListener... listeners) {
        for (DiscordListener listener : listeners) {
            for (Method method : listener.getClass().getMethods()) {
                if (method.isAnnotationPresent(EventMethod.class)) {
                    this.listeners.put(listener, method);
                    continue;
                }
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0].getSuperclass() != null &&
                        (method.getParameterTypes()[0].getSuperclass().equals(Event.class) ||
                        (method.getParameterTypes()[0].getSuperclass().getSuperclass() != null && method.getParameterTypes()[0].getSuperclass().getSuperclass().equals(Event.class))))
                    this.listeners.put(listener, method);
            }
        }
    }

    /**
     * Dispatches an event to all registered listeners.
     * This method is called by the {@link DiscordJar} class & other internal classes and should not be called by the end user.
     * This method is called in a new thread so the bot can handle multiple events at once.
     *
     * @param event The event to dispatch
     * @since 1.0
     */
    public void dispatchEvent(Event event, Class<? extends Event> type, DiscordJar djv) {
        new Thread(() -> {
            if (event instanceof MessageCreateEvent) {
                try {
                    if (((MessageCreateEvent) event).getMessage().author().id().equals(djv.getSelfUser().id()))
                        return;
                } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
                    throw new RuntimeException(e);
                }
            }

            for (DiscordListener listener : listeners.keySet()) {
                Method method = listeners.get(listener);
                if ((method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(type) && Modifier.isPublic(method.getModifiers())) || method.isAnnotationPresent(EventMethod.class)) {
                    try {
                        if (event instanceof CustomIdable) {
                            if (method.isAnnotationPresent(RequireCustomId.class)) {
                                if (!((CustomIdable) event).getCustomId().equals(method.getAnnotation(RequireCustomId.class).value()))
                                    continue;
                            }
                        }

                        method.setAccessible(true);
                        method.invoke(listener, event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "EventDispatcher").start();
    }

    public HashMap<DiscordListener, Method> getListeners() {
        return listeners;
    }
}
