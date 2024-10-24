package org.ninety;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.client.api.utils.WorldUtils;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.utils.Timer;

import java.util.List;

/**
 * AutoTorch Module
 *
 * @author _90_
 */
public class AutoTorchModule extends ToggleableModule {

	private final BooleanSetting rotateToPlace = new BooleanSetting("Rotate", false);
	private final NumberSetting<Float> range = new NumberSetting<>("Range", 5.0f, 0.0f, 6.0f).incremental(0.1f);

	List<BlockPos> validPlacements;
	BlockPos buffer = null;
	Timer timer = new Timer();

	public AutoTorchModule() {
		super("AutoTorch", "Automatically light up areas in range", ModuleCategory.WORLD);

		//register settings
		this.registerSettings(
				this.rotateToPlace,
				this.range
		);
	}

	public static boolean isInteractiveBlock(BlockPos pos) {
		BlockState blockState = mc.level.getBlockState(pos);

		BlockEntity blockEntity = mc.level.getBlockEntity(pos);
		if (blockEntity instanceof MenuProvider) {
			return true;
		}

		if (blockState.getBlock() instanceof net.minecraft.world.level.block.BaseEntityBlock) {
			return true;
		}

		return false;
	}

	private boolean validTopFace(BlockPos pos) {
		BlockState above = mc.level.getBlockState(pos.above());

		boolean belowReplaceable = above.canBeReplaced();
		boolean belowLiquid = !above.getFluidState().isEmpty();
		boolean lowLight = mc.level.getRawBrightness(pos.above(), 16) < 1;
		boolean validPlacement = Block.canSupportCenter(mc.level, pos, Direction.UP);
		boolean interactive = isInteractiveBlock(pos);

		return belowReplaceable && !belowLiquid && lowLight && validPlacement && !interactive;
	}

	private void swapPlace(BlockPos placement, Boolean rotate) {
		int torchSlot = InventoryUtils.findItem(Items.TORCH, true, true);
		int currentSlot = mc.player.getInventory().selected;
		if (torchSlot == -1) return;

		InventoryUtils.swapSlots(torchSlot,currentSlot);

		if (!mc.player.isHolding(Items.TORCH)) return;

		if (rotate) {
			RusherHackAPI.getRotationManager().updateRotation(placement, Direction.UP, 0.5f);
		}

		mc.gameMode.useItemOn(
				mc.player,
				InteractionHand.MAIN_HAND,
				new BlockHitResult(placement.getCenter().add(new Vec3(0,0.5,0)), Direction.UP, placement, false));

		InventoryUtils.swapSlots(torchSlot,currentSlot);
	}

	@Subscribe
	private void onUpdate(EventUpdate event) {
		if (buffer != null) {
			if (validTopFace(buffer)) {
				swapPlace(buffer, rotateToPlace.getValue());
			}
			buffer = null;
		}

		Vec3 eyePos = mc.player.getEyePosition();
		validPlacements = WorldUtils.getSphere(
				new Vec3i((int) eyePos.x - 1, (int) eyePos.y - 2, (int) eyePos.z),
				this.range.getValue(),
				this::validTopFace);

		if (validPlacements == null) return;
		if (validPlacements.size() < 1) return;

		double minDist = range.getValue().doubleValue() * range.getValue().doubleValue();
		BlockPos placement = null;
		for (BlockPos bp : validPlacements) {
			Vec3 targetPos = bp.getCenter();
			double dist = targetPos.distanceToSqr(mc.player.getEyePosition().subtract(new Vec3(1,2,0)));
			if (dist < minDist) {
				minDist = dist;
				placement = bp;
			}
		}
		if (placement == null) return;

		buffer = placement;
	}

	@Override
	public void onEnable() {
		timer.reset();
	}

	@Override
	public void onDisable() {}
}
