package com.mathffreitas;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class RagConfiguration {

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Produces
    public RetrievalAugmentor retrievalAugmentor() {

        System.out.println(">>> Creating RetrievalAugmentor");

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(
                        EmbeddingStoreContentRetriever.builder()
                                .embeddingStore(embeddingStore)
                                .embeddingModel(embeddingModel)
                                .maxResults(5)
                                .build())
                .build();
    }
}
