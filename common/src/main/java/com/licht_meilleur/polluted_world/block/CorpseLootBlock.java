package com.licht_meilleur.polluted_world.block;

import com.licht_meilleur.polluted_world.PollutedWorldMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;

import java.util.ArrayList;
import java.util.List;

public class CorpseLootBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CorpseLootBlock> CODEC = simpleCodec(CorpseLootBlock::new);

    public CorpseLootBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<CorpseLootBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        serverLevel.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0F, 0.9F);

        List<ItemStack> stacks = generateLoot(serverLevel, serverPlayer, pos);
        giveOrDropLoot(serverLevel, serverPlayer, pos, stacks);

        serverLevel.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        serverLevel.levelEvent(2001, pos, Block.getId(state));
        serverLevel.removeBlock(pos, false);

        return InteractionResult.CONSUME;
    }

    private List<ItemStack> generateLoot(ServerLevel level, ServerPlayer player, BlockPos pos) {
        ResourceKey<LootTable> key = ResourceKey.create(
                Registries.LOOT_TABLE,
                PollutedWorldMod.id(resolveLootTablePath())
        );

        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);

        return new ArrayList<>(table.getRandomItems(params));
    }

    private String resolveLootTablePath() {
        Identifier id = BuiltInRegistries.BLOCK.getKey(this);
        String path = id.getPath();

        return com.licht_meilleur.polluted_world.world.registry.CorpseLootRegistry.lootTableFor(path);
    }

    private void giveOrDropLoot(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            List<ItemStack> stacks
    ) {
        if (stacks.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.polluted_world.nothing_found"));
            return;
        }

        List<String> names = new ArrayList<>();

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            ItemStack remaining = stack.copy();
            names.add(stack.getHoverName().getString() + " x" + stack.getCount());

            boolean inserted = player.getInventory().add(remaining);

            if (!inserted || !remaining.isEmpty()) {
                Block.popResource(level, pos, remaining);
            }
        }

        player.sendOverlayMessage(
                Component.translatable("message.polluted_world.looted_corpse: " + String.join(" / ", names))
        );
    }
}