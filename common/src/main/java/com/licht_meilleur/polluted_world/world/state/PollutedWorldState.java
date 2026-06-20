package com.licht_meilleur.polluted_world.world.state;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PollutedWorldState extends SavedData {

    private static final int DATA_VERSION = 1;


    private static final Codec<PollutedWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("dataVersion", 0).forGetter(state -> DATA_VERSION),
            Codec.BOOL.fieldOf("generated").forGetter(PollutedWorldState::isGenerated),
            BlockPos.CODEC.optionalFieldOf("startSpawnPos", BlockPos.ZERO).forGetter(state ->
                    state.startSpawnPos == null ? BlockPos.ZERO : state.startSpawnPos
            ),
            Codec.STRING.listOf().optionalFieldOf("firstJoinedPlayers", List.of()).forGetter(state ->
                    state.firstJoinedPlayers.stream().map(UUID::toString).toList()
            )
    ).apply(instance, PollutedWorldState::new));

    public static final SavedDataType<PollutedWorldState> ID = new SavedDataType<>(
            PollutedWorldMod.id("polluted_world_state"),
            PollutedWorldState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private boolean generated;
    private BlockPos startSpawnPos;
    private final Set<UUID> firstJoinedPlayers = new HashSet<>();

    public PollutedWorldState() {
        this(DATA_VERSION, false, BlockPos.ZERO, List.of());
        this.startSpawnPos = null;
    }

    private PollutedWorldState(int dataVersion, boolean generated, BlockPos startSpawnPos, List<String> playerIds) {
        this.generated = generated;
        this.startSpawnPos = startSpawnPos.equals(BlockPos.ZERO) ? null : startSpawnPos;

        for (String id : playerIds) {
            try {
                this.firstJoinedPlayers.add(UUID.fromString(id));
            } catch (Exception ignored) {
            }
        }
    }

    public static PollutedWorldState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ID);
    }

    public boolean isGenerated() {
        return generated;
    }

    public void markGenerated(BlockPos startSpawnPos) {
        this.generated = true;
        this.startSpawnPos = startSpawnPos;
        setDirty();
    }

    public BlockPos startSpawnPos() {
        return startSpawnPos;
    }

    public boolean hasFirstJoined(UUID uuid) {
        return firstJoinedPlayers.contains(uuid);
    }

    public void markFirstJoined(UUID uuid) {
        firstJoinedPlayers.add(uuid);
        setDirty();
    }
}