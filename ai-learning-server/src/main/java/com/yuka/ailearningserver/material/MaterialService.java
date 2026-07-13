package com.yuka.ailearningserver.material;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.common.OwnershipGuard;
import com.yuka.ailearningserver.material.dto.CreateMaterialRequest;
import com.yuka.ailearningserver.material.dto.MaterialResponse;
import com.yuka.ailearningserver.material.dto.UpdateMaterialRequest;
import com.yuka.ailearningserver.material.entity.LearningMaterial;
import com.yuka.ailearningserver.material.entity.MaterialType;
import com.yuka.ailearningserver.material.mapper.LearningMaterialMapper;
import com.yuka.ailearningserver.subject.SubjectService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Materials are existentially owned by a subject, so every subject-scoped
 * operation validates subject ownership first. This phase covers metadata and
 * external links only — uploads arrive with the future StorageService
 * ({@code storageKey}/{@code sizeBytes} stay reserved).
 */
@Service
public class MaterialService {

    private final LearningMaterialMapper materialMapper;
    private final SubjectService subjectService;

    public MaterialService(LearningMaterialMapper materialMapper, SubjectService subjectService) {
        this.materialMapper = materialMapper;
        this.subjectService = subjectService;
    }

    public List<MaterialResponse> listBySubject(Long userId, Long subjectId) {
        subjectService.requireOwned(userId, subjectId);
        return materialMapper.selectList(new LambdaQueryWrapper<LearningMaterial>()
                        .eq(LearningMaterial::getSubjectId, subjectId)
                        .orderByDesc(LearningMaterial::getCreatedAt))
                .stream()
                .map(MaterialResponse::from)
                .toList();
    }

    public MaterialResponse create(Long userId, Long subjectId, CreateMaterialRequest request) {
        subjectService.requireOwned(userId, subjectId);
        LearningMaterial material = new LearningMaterial();
        material.setSubjectId(subjectId);
        material.setUserId(userId);
        material.setTitle(request.title());
        material.setType(MaterialType.valueOf(request.type().toUpperCase(Locale.ROOT)));
        material.setDescription(request.description());
        material.setSourceUrl(request.sourceUrl());
        materialMapper.insert(material);
        return MaterialResponse.from(material);
    }

    public MaterialResponse update(Long userId, Long id, UpdateMaterialRequest request) {
        LearningMaterial material = requireOwned(userId, id);
        if (request.title() != null && !request.title().isBlank()) {
            material.setTitle(request.title());
        }
        if (request.type() != null) {
            material.setType(MaterialType.valueOf(request.type().toUpperCase(Locale.ROOT)));
        }
        if (request.description() != null) {
            material.setDescription(request.description());
        }
        if (request.sourceUrl() != null) {
            material.setSourceUrl(request.sourceUrl());
        }
        materialMapper.updateById(material);
        return MaterialResponse.from(material);
    }

    public void delete(Long userId, Long id) {
        LearningMaterial material = requireOwned(userId, id);
        materialMapper.deleteById(material.getId());
    }

    private LearningMaterial requireOwned(Long userId, Long id) {
        return OwnershipGuard.require(materialMapper.selectById(id), LearningMaterial::getUserId, userId,
                MaterialErrorCode.MATERIAL_NOT_FOUND, MaterialErrorCode.MATERIAL_ACCESS_DENIED);
    }
}
