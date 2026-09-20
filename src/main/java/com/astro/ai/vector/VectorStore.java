package com.astro.ai.vector;

import com.astro.ai.model.VectorDocument;

import java.util.List;

public interface VectorStore {

    void add(VectorDocument document);

    void addAll(List<VectorDocument> documents);

    List<VectorDocument> similaritySearch(float[] queryVector, int topK, String traditionFilter, String categoryFilter);

    int size();
}
