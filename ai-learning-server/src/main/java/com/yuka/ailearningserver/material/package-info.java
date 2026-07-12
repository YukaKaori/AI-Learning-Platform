/**
 * Learning materials — the content a subject is studied from: PDFs, markdown
 * documents, videos, articles, external links.
 *
 * <p>Phase 5 is schema + entity only. Upload/ingest goes through the
 * {@code infrastructure} {@code StorageService} seam when implemented (the
 * entity carries a {@code storageKey} for that future); AI ingestion
 * (chunking/embedding for retrieval) is a Phase 6+ concern layered on top —
 * never inside — this package. Reserved error-code range: 120000–129999.
 */
package com.yuka.ailearningserver.material;
