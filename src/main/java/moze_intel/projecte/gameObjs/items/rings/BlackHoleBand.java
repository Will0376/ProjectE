package moze_intel.projecte.gameObjs.items.rings;

import com.google.common.collect.Lists;
import moze_intel.projecte.api.item.IAlchBagItem;
import moze_intel.projecte.api.item.IAlchChestItem;
import moze_intel.projecte.api.item.IPedestalItem;
import moze_intel.projecte.gameObjs.tiles.AlchChestTile;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.BlockFlowingFluid;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.fluid.Fluid;
import net.minecraft.init.Fluids;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class BlackHoleBand extends RingToggle implements IAlchBagItem, IAlchChestItem, IPedestalItem
{
	public BlackHoleBand(Properties props)
	{
		super(props);
	}

	private EnumActionResult tryPickupFluid(World world, EntityPlayer player, ItemStack stack)
	{
		RayTraceResult rtr = this.rayTrace(world, player, true);

		if (rtr == null || rtr.type != RayTraceResult.Type.BLOCK)
			return EnumActionResult.PASS;

		BlockPos fluidPos = rtr.getBlockPos();
		IBlockState state = world.getBlockState(fluidPos);

		if (world.isBlockModifiable(player, fluidPos) && player.canPlayerEdit(fluidPos, rtr.sideHit, stack)
				&& state.getBlock() instanceof IBucketPickupHandler)
		{
			Fluid fluid = ((IBucketPickupHandler) state.getBlock()).pickupFluid(world, fluidPos, state);
			if (fluid != Fluids.EMPTY)
			{
				player.playSound(fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
				return EnumActionResult.SUCCESS;
			}
		}
		return EnumActionResult.PASS;
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand)
	{
		if (tryPickupFluid(world, player, player.getHeldItem(hand)) != EnumActionResult.SUCCESS)
		{
			changeMode(player, player.getHeldItem(hand), hand);
		}
		
		return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
	}
	
	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean held)
	{
        if (!stack.getOrCreateTag().getBoolean(TAG_ACTIVE) || !(entity instanceof EntityPlayer))
		{
			return;
		}
		
		EntityPlayer player = (EntityPlayer) entity;
		AxisAlignedBB bBox = player.getBoundingBox().grow(7);
		List<EntityItem> itemList = world.getEntitiesWithinAABB(EntityItem.class, bBox);
		
		for (EntityItem item : itemList)
		{
			if (ItemHelper.hasSpace(player.inventory.mainInventory, item.getItem()))
			{
				WorldHelper.gravitateEntityTowards(item, player.posX, player.posY, player.posZ);
			}
		}
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos)
	{
		DMPedestalTile tile = ((DMPedestalTile) world.getTileEntity(pos));
		if (tile != null)
		{
			List<EntityItem> list = world.getEntitiesWithinAABB(EntityItem.class, tile.getEffectBounds());
			for (EntityItem item : list)
			{
				WorldHelper.gravitateEntityTowards(item, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
				if (!world.isRemote && item.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 1.21 && item.isAlive())
				{
					suckDumpItem(item, tile);
				}
			}
		}
	}

	private void suckDumpItem(EntityItem item, DMPedestalTile tile)
	{
		Map<EnumFacing, TileEntity> map = WorldHelper.getAdjacentTileEntitiesMapped(tile.getWorld(), tile);
		for (Map.Entry<EnumFacing, TileEntity> e : map.entrySet())
		{
			IItemHandler inv = e.getValue().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, e.getKey()).orElse(null);

			if (inv == null && e.getValue() instanceof IInventory)
			{
				inv = new InvWrapper((IInventory) e.getValue());
			}

			ItemStack result = ItemHandlerHelper.insertItemStacked(inv, item.getItem(), false);

			if (result.isEmpty())
			{
				item.remove();
				return;
			}
			else
			{
				item.setItem(result);
			}
		}
	}

	@Nonnull
	@Override
	public List<ITextComponent> getPedestalDescription()
	{
		return Lists.newArrayList(
				new TextComponentTranslation("pe.bhb.pedestal1").applyTextStyle(TextFormatting.BLUE),
				new TextComponentTranslation("pe.bhb.pedestal2").applyTextStyle(TextFormatting.BLUE)
		);
	}

	@Override
	public void updateInAlchChest(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull ItemStack stack)
	{
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof AlchChestTile))
		{
			return;
		}
		AlchChestTile tile = (AlchChestTile) te;
        if (stack.getOrCreateTag().getBoolean(TAG_ACTIVE))
		{
			AxisAlignedBB aabb = new AxisAlignedBB(tile.getPos().getX() - 5, tile.getPos().getY() - 5, tile.getPos().getZ() - 5,
					tile.getPos().getX() + 5, tile.getPos().getY() + 5, tile.getPos().getZ() + 5);
			double centeredX = tile.getPos().getX() + 0.5;
			double centeredY = tile.getPos().getY() + 0.5;
			double centeredZ = tile.getPos().getZ() + 0.5;

			for (EntityItem e : tile.getWorld().getEntitiesWithinAABB(EntityItem.class, aabb))
			{
				WorldHelper.gravitateEntityTowards(e, centeredX, centeredY, centeredZ);
				if (!e.getEntityWorld().isRemote && e.isAlive() && e.getDistanceSq(centeredX, centeredY, centeredZ) < 1.21)
				{
					tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(inv -> {
						ItemStack result = ItemHandlerHelper.insertItemStacked(inv, e.getItem(), false);
						if (!result.isEmpty())
						{
							e.setItem(result);
						}
						else
						{
							e.remove();
						}
					});
				}
			}
		}
	}

	@Override
	public boolean updateInAlchBag(@Nonnull IItemHandler inv, @Nonnull EntityPlayer player, @Nonnull ItemStack stack)
	{
        if (stack.getOrCreateTag().getBoolean(TAG_ACTIVE))
		{
			for (EntityItem e : player.getEntityWorld().getEntitiesWithinAABB(EntityItem.class, player.getBoundingBox().grow(5)))
			{
				WorldHelper.gravitateEntityTowards(e, player.posX, player.posY, player.posZ);
			}
		}
		return false;
	}
}
