package com.seailz.javadiscordwrapper.events.model.interaction.select.entity;

import com.seailz.javadiscordwrapper.DiscordJv;
import com.seailz.javadiscordwrapper.events.model.interaction.InteractionEvent;
import com.seailz.javadiscordwrapper.model.channel.Channel;
import com.seailz.javadiscordwrapper.model.component.select.SelectOption;
import com.seailz.javadiscordwrapper.model.component.select.entitiy.ChannelSelectMenu;
import com.seailz.javadiscordwrapper.model.component.select.string.StringSelectMenu;
import com.seailz.javadiscordwrapper.model.interaction.data.message.MessageComponentInteractionData;
import com.seailz.javadiscordwrapper.model.role.Role;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChannelSelectMenuInteractionEvent extends InteractionEvent {

    public ChannelSelectMenuInteractionEvent(DiscordJv bot, long sequence, JSONObject data) {
        super(bot, sequence, data);
    }

    /**
     * Returns the interaction data that was created from the event.
     *
     * This SHOULD not ever return null.
     * @return {@link com.seailz.javadiscordwrapper.model.interaction.data.message.MessageComponentInteractionData} object containing the interaction data.
     */
    @NotNull
    public MessageComponentInteractionData getInteractionData() {
        return (MessageComponentInteractionData) getInteraction().data();
    }


    /**
     * Returns the selected channels of the {@link com.seailz.javadiscordwrapper.model.component.select.entitiy.ChannelSelectMenu ChannelSelectMenu}.
     *
     * @throws IllegalStateException if the event was not fied in a {@link com.seailz.javadiscordwrapper.model.guild.Guild Guild}.
     *
     * @return A list of {@link com.seailz.javadiscordwrapper.model.channel.Channel} objects containing the selected channels.
     */
    public List<Channel> getSelectedChannels() {
        if (getGuild() == null)
            throw new IllegalArgumentException("This event was not fired in a guild.");

        List<Channel> channels = new ArrayList<>();
        getInteractionData().snowflakes().stream()
                .map(getBot()::getChannelById)
                .forEach(channels::add);
        return channels;
    }

    /**
     * Returns the custom id of the select menu.
     *
     * This SHOULD not ever return null.
     * @return {@link String} object containing the custom id.
     */
    @NotNull
    public String getCustomId() {
        return getInteractionData().customId();
    }

    /**
     * Retrieves the {@link com.seailz.javadiscordwrapper.model.component.select.entitiy.ChannelSelectMenu ChannelSelectMenu} component
     */
    @NotNull
    public ChannelSelectMenu getComponent() {
        return (ChannelSelectMenu) getInteraction().message().components().stream()
                .filter(component -> component instanceof ChannelSelectMenu)
                .filter(component -> ((ChannelSelectMenu) component).customId().equals(getCustomId()))
                .findFirst()
                .orElseThrow();
    }
}
