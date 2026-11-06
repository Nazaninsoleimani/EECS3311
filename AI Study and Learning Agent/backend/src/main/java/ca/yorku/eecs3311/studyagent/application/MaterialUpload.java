package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;

/**
 * Result of uploading a material (UC02, which includes UC04): the stored material and the concepts
 * extracted from it right after indexing. concepts is null when extraction did not run or failed;
 * conceptWarning then says why, while the material itself stays usable.
 */
public record MaterialUpload(CourseMaterial material, ConceptGraph concepts, String conceptWarning) {
    public boolean conceptsExtracted() { return concepts != null; }
}
