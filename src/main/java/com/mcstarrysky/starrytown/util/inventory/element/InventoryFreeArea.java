package com.mcstarrysky.starrytown.util.inventory.element;

import com.mcstarrysky.starrytown.util.LocaleUtil;
import com.mcstarrysky.starrytown.util.inventory.InventoryElement;
import com.mcstarrysky.starrytown.util.inventory.PInventory;
import lombok.Builder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@Data
public class InventoryFreeArea implements InventoryElement {

    private final JavaPlugin plugin;
    private final BaseElement base;
    private final Consumer<InventoryClickEvent> onClick;
    private final PInventory<?> holder;
    private final Map<Integer, ItemStack> placedMap = new HashMap<>();

    @Builder
    public InventoryFreeArea(BaseElement base, Consumer<InventoryClickEvent> onClick, PInventory<?> holder) {
        this.base = base;
        this.onClick = onClick;
        this.holder = holder;

        this.plugin = holder.getPlugin();
    }

    @Nullable
    public static InventoryFreeArea get(final PInventory<?> inv, final String name) {
        return (InventoryFreeArea) inv.getElement(name);
    }

    public Map<Integer, ItemStack> getPlacedMap() {
        refresh();
        return placedMap;
    }

    public void onClick(final InventoryClickEvent event) {
        if (Objects.isNull(onClick)) {
            return;
        }
        onClick.accept(event);
    }

    @Nullable
    public ItemStack getPlaced(final int slot) {
        final ItemStack item = this.placedMap.get(slot);
        if (Objects.isNull(item)) {
            return null;
        }
        return item.clone();
    }

    @NotNull
    public List<ItemStack> addPlaced(final ItemStack... items) {
        refresh();
        final List<ItemStack> result = new ArrayList<>();
        if (Objects.isNull(items) || items.length <= 0) {
            return result;
        }

        final Iterator<ItemStack> iterator = Arrays.asList(items).iterator();
        for (int slot : getPositions()) {
            if (!iterator.hasNext()) {
                break;
            }
            if (this.placedMap.containsKey(slot)) {
                continue;
            }
            this.placedMap.put(slot, iterator.next().clone());
        }

        while (iterator.hasNext()) {
            result.add(iterator.next().clone());
        }
        return result;
    }

    public boolean addPlaced(final int slot, @Nullable final ItemStack item, final boolean force) {
        if (force) {
            this.placedMap.put(slot, item);
            return true;
        } else {
            return Objects.isNull(this.placedMap.putIfAbsent(slot, item));
        }
    }

    public void clear() {
        this.placedMap.clear();
    }

    public void refresh() {
        LocaleUtil.debug("刷新 InventoryFreeArea: {0}", getBase().getName());
        LocaleUtil.debug("初始数据集: {0}", this.placedMap.size());

        this.placedMap.clear();
        final Inventory inv = holder.getInventory();
        for (int slot : base.getPositions()) {
            final ItemStack item = inv.getItem(slot);
            if (Objects.isNull(item) || item.getType() == Material.AIR) {
                continue;
            }
            this.placedMap.put(slot, item);
        }
        LocaleUtil.debug("过滤后数据集: {0}", this.placedMap.size());
    }

    @Override
    public ItemStack parseItem(PInventory<?> inv, int slot) {
        return this.placedMap.getOrDefault(slot, this.base.getItem().get());
    }

    @Override
    public void click(PInventory<?> holder, InventoryClickEvent event) {
        onClick(event);
        Bukkit.getScheduler().runTask(plugin, this::refresh);
    }

    @Override
    public @NotNull BaseElement getBase() {
        return this.base;
    }

    @Override
    public boolean isClickable() {
        return true;
    }
}
