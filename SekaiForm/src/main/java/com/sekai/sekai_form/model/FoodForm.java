package com.sekai.sekai_form.model;

public class FoodForm {
    private String name;
    private String description;
    private Integer satietyValue;
    private Integer moodValue;
    private Integer affectionValue;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSatietyValue() { return satietyValue; }
    public void setSatietyValue(Integer satietyValue) { this.satietyValue = satietyValue; }
    public Integer getMoodValue() { return moodValue; }
    public void setMoodValue(Integer moodValue) { this.moodValue = moodValue; }
    public Integer getAffectionValue() { return affectionValue; }
    public void setAffectionValue(Integer affectionValue) { this.affectionValue = affectionValue; }
}