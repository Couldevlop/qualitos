package com.openlab.qualitos.quality.training;

import java.util.UUID;

public class SkillNotFoundException extends RuntimeException {
    public SkillNotFoundException(UUID id) { super("Skill not found: " + id); }
}
