package com.sekai.sekai_form.dataobject;

public class SekaiFormCharacterDO {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String modelType;
    private String modelPath;
    private Integer satiety;
    private Integer mood;
    private Integer affection;
    private Integer level;
    private Integer exp;
    private Integer freePoints;
    private Integer hp;
    private Integer atk;
    private Integer def;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }
    public Integer getSatiety() { return satiety; }
    public void setSatiety(Integer satiety) { this.satiety = satiety; }
    public Integer getMood() { return mood; }
    public void setMood(Integer mood) { this.mood = mood; }
    public Integer getAffection() { return affection; }
    public void setAffection(Integer affection) { this.affection = affection; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getExp() { return exp; }
    public void setExp(Integer exp) { this.exp = exp; }
    public Integer getFreePoints() { return freePoints; }
    public void setFreePoints(Integer freePoints) { this.freePoints = freePoints; }
    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }
    public Integer getAtk() { return atk; }
    public void setAtk(Integer atk) { this.atk = atk; }
    public Integer getDef() { return def; }
    public void setDef(Integer def) { this.def = def; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
}