package com.sunshulkers.utils;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.block.Action;

/**
 * Режимы открытия шалкеров
 */
public enum OpenMode {
    RIGHT_CLICK("RIGHT_CLICK", false, false, true),
    SHIFT_RIGHT_CLICK("SHIFT_RIGHT_CLICK", true, false, true),
    LEFT_CLICK("LEFT_CLICK", false, true, false),
    SHIFT_LEFT_CLICK("SHIFT_LEFT_CLICK", true, true, false),
    ANY_CLICK("ANY_CLICK", false, true, true), // Любой клик (левый или правый)
    SHIFT_ANY_CLICK("SHIFT_ANY_CLICK", true, true, true); // Shift + любой клик
    
    private final String name;
    private final boolean requireShift;
    private final boolean leftClick;
    private final boolean rightClick;
    
    OpenMode(String name, boolean requireShift, boolean leftClick, boolean rightClick) {
        this.name = name;
        this.requireShift = requireShift;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }
    
    /**
     * Проверяет, соответствует ли клик в инвентаре этому режиму
     */
    public boolean matchesInventoryClick(ClickType clickType) {
        // Проверяем Shift
        if (requireShift && !clickType.isShiftClick()) {
            return false;
        }
        if (!requireShift && clickType.isShiftClick()) {
            return false;
        }
        
        // Проверяем тип клика
        switch (this) {
            case RIGHT_CLICK:
            case SHIFT_RIGHT_CLICK:
                return clickType.isRightClick();
            
            case LEFT_CLICK:
            case SHIFT_LEFT_CLICK:
                return clickType.isLeftClick();
            
            case ANY_CLICK:
            case SHIFT_ANY_CLICK:
                return clickType.isLeftClick() || clickType.isRightClick();
            
            default:
                return false;
        }
    }
    
    /**
     * Проверяет, соответствует ли действие в руке этому режиму
     */
    public boolean matchesHandAction(Action action, boolean isSneaking) {
        // Проверяем Shift
        if (requireShift != isSneaking) {
            return false;
        }
        
        // Проверяем действие
        if (leftClick && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            return true;
        }
        
        if (rightClick && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Получает режим по имени
     */
    public static OpenMode fromString(String name) {
        for (OpenMode mode : values()) {
            if (mode.name.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return name;
    }
} 