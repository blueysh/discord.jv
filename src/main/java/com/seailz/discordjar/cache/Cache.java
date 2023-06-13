package com.seailz.discordjar.cache;

import com.seailz.discordjar.DiscordJar;
import com.seailz.discordjar.model.guild.Guild;
import com.seailz.discordjar.model.guild.Member;
import com.seailz.discordjar.utils.rest.DiscordRequest;
import com.seailz.discordjar.utils.rest.DiscordResponse;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Cache object used to store objects in memory
 *
 * @param <T> The type of object to store
 * @author Seailz
 * @see DiscordJar
 * @since 1.0
 */
public class Cache<T> {

    private final List<T> cache = new ArrayList<>();
    private final DiscordJar discordJar;
    private final Class<T> clazz;
    private final DiscordRequest discordRequest;
    private final boolean isMember;
    private final Guild guild;

    public Cache(DiscordJar discordJar, Class<T> clazz, DiscordRequest request, Guild guild) {
        this.discordJar = discordJar;
        this.clazz = clazz;
        this.discordRequest = request;
        this.guild = guild;
        isMember = clazz == Member.class;

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                cache.clear();
            }
        }).start();
    }

    public Cache(DiscordJar discordJar, Class<T> clazz, DiscordRequest request) {
        this(discordJar, clazz, request, null);
    }

    /**
     * Adds an object to the cache
     *
     * @param t The object to add
     */
    public void cache(@NotNull T t)  {
        String id;
        try {
            if (isMember) {
                id = ((Member) t).user().id();
            } else id = (String) t.getClass().getMethod("id").invoke(t);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        for (T cacheMember : cache) {
            String cacheId;
            try {
                if (isMember) {
                    cacheId = ((Member) cacheMember).user().id();
                } else cacheId = (String) t.getClass().getMethod("id").invoke(t);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            if (cacheId.equals(id)) {
                cache.remove(cacheMember);
                break;
            }
        }
        cache.add(t);
    }

    /**
     * Removes an item from the cache
     *
     * @param t The item to remove
     */
    public void remove(T t) {
        cache.remove(t);
    }

    public void removeById(String id) {
        remove(getFromCacheByIdOrNull(id));
    }

    /**
     * Returns the entire cache
     */
    public List<T> getCache() {
        return cache;
    }

    /**
     * Gets an item from the cache
     *
     * @param id The id of the item to get
     * @return The item
     */
    public T getById(String id) throws DiscordRequest.UnhandledDiscordAPIErrorException {
        AtomicReference<Object> returnObject = new AtomicReference<>();
        ArrayList<T> cacheCopy = new ArrayList<>(cache);
        cacheCopy.forEach(t -> {
            String itemId;

            if (isMember) {
                itemId = ((Member) t).user().id();
                if (Objects.equals(itemId, id))
                    returnObject.set(t);
            } else {
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals("id")) {
                        try {
                            itemId = (String) method.invoke(t);
                            if (Objects.equals(itemId, id)) {
                                returnObject.set(t);
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });


        if (returnObject.get() == null) {
            // request from discord
            DiscordResponse response;
                response = new DiscordRequest(
                        discordRequest.body(), discordRequest.headers(), discordRequest.url().replaceAll("%s", id), discordJar, discordRequest.url(), RequestMethod.GET
                ).invoke();
            Method decompile;
            try {
                decompile = clazz.getMethod("decompile", JSONObject.class, DiscordJar.class);
            } catch (NoSuchMethodException e) {
                try {
                    decompile = clazz.getMethod("decompile", JSONObject.class);
                } catch (NoSuchMethodException ex) {
                    try {
                        decompile = clazz.getMethod("decompile", JSONObject.class, DiscordJar.class, String.class, Guild.class);
                    } catch (NoSuchMethodException exx) {
                        Logger.getLogger("DiscordJar").severe("Was unable to return object from cache, please report this to discord.jar's github!");
                        throw new RuntimeException(exx);
                    }
                }
            }

            try {
                if (response == null) return null;
                returnObject.set(decompile.invoke(null, response.body(), discordJar));
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                Logger.getLogger("DiscordJar").warning("Was unable to return object from cache, attempting to remove discord.jar instance...");
                try {
                    returnObject.set(decompile.invoke(null, response.body()));
                    Logger.getLogger("discord.jar").info("Successfully retrieved object from cache!");
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                    try {
                        returnObject.set(decompile.invoke(null, response.body(), discordJar, guild.id(), guild));
                    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e1) {
                        Logger.getLogger("DiscordJar").severe("Was unable to return object from cache, please report this to discord.jar's github!");
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        if (returnObject.get() != null) cache((T) returnObject.get());
        return returnObject.get() == null ? null : (T) returnObject.get();
    }

    private T getFromCacheByIdOrNull(String id) {
        AtomicReference<Object> returnObject = new AtomicReference<>();
        ArrayList<T> cacheCopy = new ArrayList<>(cache);
        cacheCopy.forEach(t -> {
            String itemId;

            if (isMember) {
                itemId = ((Member) t).user().id();
                if (Objects.equals(itemId, id))
                    returnObject.set(t);
            } else {
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals("id")) {
                        try {
                            itemId = (String) method.invoke(t);
                            if (Objects.equals(itemId, id)) {
                                returnObject.set(t);
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        return returnObject.get() == null ? null : (T) returnObject.get();
    }

    public JSONObject getFresh(String id) {
        DiscordResponse response = null;
        try {
            response = new DiscordRequest(
                    discordRequest.body(), discordRequest.headers(), discordRequest.url().replaceAll("%s", id), discordJar, discordRequest.url(), RequestMethod.GET
            ).invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return response.body();
    }

    public void clear() {
        cache.clear();
    }
}
