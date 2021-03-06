package com.infinityraider.ninjagear.block;

import com.infinityraider.ninjagear.api.v1.IRopeAttachable;
import com.infinityraider.ninjagear.item.ItemRope;
import com.infinityraider.ninjagear.reference.Constants;
import com.infinityraider.ninjagear.reference.Reference;
import com.infinityraider.infinitylib.block.BlockBase;
import com.infinityraider.infinitylib.block.ICustomRenderedBlock;
import com.infinityraider.infinitylib.block.blockstate.InfinityProperty;
import com.infinityraider.infinitylib.render.block.IBlockRenderingHandler;
import com.infinityraider.ninjagear.registry.ItemRegistry;
import com.infinityraider.ninjagear.render.block.RenderBlockRope;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@MethodsReturnNonnullByDefault
public class BlockRope extends BlockBase implements ICustomRenderedBlock, IRopeAttachable {
    private final AxisAlignedBB box;

    public BlockRope() {
        super("ropeBlock", Material.VINE);
        float u = Constants.UNIT;
        this.box = new AxisAlignedBB(7.5*u, 0, 7.5*u, 8.5*u, 1, 8.5*u);
        this.setCreativeTab(null);
    }

    @Override
    public List<String> getOreTags() {
        return Collections.emptyList();
    }

    @Override
    protected InfinityProperty[] getPropertyArray() {
        return new InfinityProperty[0];
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if(!this.canRopeStay(world, pos)) {
            this.breakRope(world, pos, state, false);
        }
    }

    public boolean canRopeStay(World world, BlockPos pos) {
        BlockPos up = pos.up();
        IBlockState state = world.getBlockState(up);
        if(state.isSideSolid(world, up, EnumFacing.DOWN)) {
            return true;
        }
        if(state.getBlock() instanceof IRopeAttachable) {
            IRopeAttachable attachable = (IRopeAttachable) state.getBlock();
            return attachable.canAttachRope(world, up, state);
        }
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && canRopeStay(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        BlockPos up = pos.up();
        IBlockState stateUp = world.getBlockState(up);
        if(stateUp.getBlock() instanceof IRopeAttachable) {
            ((IRopeAttachable) stateUp.getBlock()).onRopeAttached(world, up, stateUp);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (!world.isRemote && heldItem.getItem() instanceof ItemRope) {
            if (this.extendRope(world, pos) && !player.capabilities.isCreativeMode) {
                player.inventory.decrStackSize(player.inventory.currentItem, 1);
            }
        }
        return false;
    }

    public boolean extendRope(World world, BlockPos pos) {
        BlockPos below = pos.down();
        IBlockState state = world.getBlockState(below);
        if(state.getBlock() instanceof BlockRope) {
            return ((BlockRope) state.getBlock()).extendRope(world, below);
        }
        if(this.canPlaceBlockAt(world, below)) {
            world.setBlockState(below, this.getDefaultState(), 3);
            return true;
        }
        return false;
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        this.breakRope(world, pos, world.getBlockState(pos), player.isSneaking());
    }

    public void breakRope(World world, BlockPos pos, IBlockState state, boolean propagateUp) {
        if (propagateUp) {
            this.propagateRopeBreak(world, pos, true);
        } else {
            world.setBlockToAir(pos);
            this.dropBlockAsItem(world, pos, state, 0);
        }
    }

    private void propagateRopeBreak(World world, BlockPos pos, boolean up) {
        if(!world.isRemote) {
            BlockPos posAt = pos.add(0, up ? 1 : -1, 0);
            IBlockState state = world.getBlockState(posAt);
            world.setBlockToAir(pos);
            this.dropBlockAsItem(world, pos, state, 0);
            if (state.getBlock() instanceof BlockRope) {
                ((BlockRope) state.getBlock()).propagateRopeBreak(world, posAt, up);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.box;
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(ItemRegistry.getInstance().itemRope);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ItemRegistry.getInstance().itemRope;
    }

    @Override
    public boolean canSpawnInBlock() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP || side == EnumFacing.DOWN;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return true;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IBlockRenderingHandler getRenderer() {
        return new RenderBlockRope(this);
    }

    @Override
    public ModelResourceLocation getBlockModelResourceLocation() {
        return new ModelResourceLocation(Reference.MOD_ID, this.getInternalName());
    }

    @Override
    public boolean canAttachRope(World world, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void onRopeAttached(World world, BlockPos pos, IBlockState state) {}
}
