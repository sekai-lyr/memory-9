package com.sekai.sekai_form.dataobject;

public class CharacterStatsDO {
    private Long id;
    private Long characterId;
    private Integer hp;
    private Integer atk;
    private Integer def;
    private Integer level;
    private Integer exp;
    private Integer freePoints;
    private java.time.LocalDateTime updateTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCharacterId() { return characterId; }
    public void setCharacterId(Long characterId) { this.characterId = characterId; }
    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }
    public Integer getAtk() { return atk; }
    public void setAtk(Integer atk) { this.atk = atk; }
    public Integer getDef() { return def; }
    public void setDef(Integer def) { this.def = def; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getExp() { return exp; }
    public void setExp(Integer exp) { this.exp = exp; }
    public Integer getFreePoints() { return freePoints; }
    public void setFreePoints(Integer freePoints) { this.freePoints = freePoints; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
}