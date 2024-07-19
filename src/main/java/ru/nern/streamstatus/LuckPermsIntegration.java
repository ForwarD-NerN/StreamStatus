package ru.nern.streamstatus;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import net.minecraft.server.network.ServerPlayerEntity;

public class LuckPermsIntegration {
    public static final LuckPerms luckPerms = LuckPermsProvider.get();
    public static PrefixNode STREAMER_PREFIX = PrefixNode.builder(StreamStatus.config.streamerPrefix, StreamStatus.config.streamerPrefixPriority).build();

    public static void addStreamerPrefix(ServerPlayerEntity player){
        User user = luckPerms.getUserManager().getUser(player.getUuid());
        if(user != null){
            user.data().add(STREAMER_PREFIX);
            luckPerms.getUserManager().saveUser(user);
        }
    }

    public static void rebuildPrefix(){
        STREAMER_PREFIX = PrefixNode.builder(StreamStatus.config.streamerPrefix, StreamStatus.config.streamerPrefixPriority).build();
    }

    public static void removeStreamerPrefix(ServerPlayerEntity player){
        User user = luckPerms.getUserManager().getUser(player.getUuid());
        if(user != null){
            user.data().remove(STREAMER_PREFIX);
            luckPerms.getUserManager().saveUser(user);
        }
    }
}
