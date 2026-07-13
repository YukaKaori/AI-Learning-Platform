package com.yuka.ailearningserver.material.dto;

import com.yuka.ailearningserver.material.entity.LearningMaterial;

import java.time.ZoneId;

public record MaterialResponse(String id, String subjectId, String title, String type, String description,
                               String sourceUrl, Long sizeBytes, long createdAt) {

    public static MaterialResponse from(LearningMaterial material) {
        return new MaterialResponse(
                String.valueOf(material.getId()),
                String.valueOf(material.getSubjectId()),
                material.getTitle(),
                material.getType().name().toLowerCase(),
                material.getDescription(),
                material.getSourceUrl(),
                material.getSizeBytes(),
                material.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
