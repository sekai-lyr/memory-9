package com.sekai.sekai_form.dataobject;

public class Live2DDialogueDO {
    private Long id;
    private Long modelId;
    private String category;
    private String text;
    private String motionName;
    private String expressionName;
    private java.time.LocalDateTime createTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getMotionName() { return motionName; }
    public void setMotionName(String motionName) { this.motionName = motionName; }
    public String getExpressionName() { return expressionName; }
    public void setExpressionName(String expressionName) { this.expressionName = expressionName; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}