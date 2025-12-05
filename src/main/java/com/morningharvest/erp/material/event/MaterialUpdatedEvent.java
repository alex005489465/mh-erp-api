package com.morningharvest.erp.material.event;

import com.morningharvest.erp.common.event.BaseEvent;
import com.morningharvest.erp.material.dto.MaterialDTO;
import lombok.Getter;

import java.util.Objects;

/**
 * 原物料更新事件
 *
 * 當原物料被更新時發布此事件，包含更新前後的完整資料
 */
@Getter
public class MaterialUpdatedEvent extends BaseEvent {

    private final MaterialDTO before;
    private final MaterialDTO after;

    public MaterialUpdatedEvent(MaterialDTO before, MaterialDTO after) {
        super("MATERIAL");
        this.before = before;
        this.after = after;
    }

    public Long getMaterialId() {
        return after.getId();
    }

    public boolean isCodeChanged() {
        return !Objects.equals(before.getCode(), after.getCode());
    }

    public boolean isNameChanged() {
        return !Objects.equals(before.getName(), after.getName());
    }

    public boolean isUnitChanged() {
        return !Objects.equals(before.getUnit(), after.getUnit());
    }

    public String getOldCode() {
        return before.getCode();
    }

    public String getNewCode() {
        return after.getCode();
    }

    public String getOldName() {
        return before.getName();
    }

    public String getNewName() {
        return after.getName();
    }

    public String getOldUnit() {
        return before.getUnit();
    }

    public String getNewUnit() {
        return after.getUnit();
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, materialId=%d, codeChanged=%s, nameChanged=%s, unitChanged=%s]",
                getEventType(), getEventId(), getMaterialId(), isCodeChanged(), isNameChanged(), isUnitChanged());
    }
}
