package io.nextskip.contests.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.CacheRefreshEvent;
import io.nextskip.contests.internal.ContestCalendarClient;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestRefreshService.
 */
@ExtendWith(MockitoExtension.class)
class ContestRefreshServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ContestCalendarClient contestClient;

    @Mock
    private ContestRepository repository;

    @Mock
    private LoadingCache<String, List<Contest>> contestsCache;

    private ContestRefreshService service;

    @BeforeEach
    void setUp() {
        service = new ContestRefreshService(eventPublisher, contestClient, repository, contestsCache);
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        service.executeRefresh();

        verify(contestClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositoryDeleteAll() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        service.executeRefresh();

        verify(repository).deleteAll();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ContestEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getName()).isEqualTo("CQ WW DX Contest");
    }

    @Test
    void testExecuteRefresh_PublishesCacheRefreshEvent() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        service.executeRefresh();

        // Verify event is published
        ArgumentCaptor<CacheRefreshEvent> captor = ArgumentCaptor.forClass(CacheRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        CacheRefreshEvent event = captor.getValue();
        assertThat(event.cacheName()).isEqualTo("contests");

        // Verify the refresh action calls the cache
        event.refreshAction().run();
        verify(contestsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(contestClient.fetch()).thenThrow(new RuntimeException("Contest API error"));

        assertThatThrownBy(() -> service.executeRefresh())
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).deleteAll();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_DeletesAllAndSavesNothing() {
        when(contestClient.fetch()).thenReturn(Collections.emptyList());

        service.executeRefresh();

        verify(repository).deleteAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testExtractWa7bnmRef_ValidUrl_ReturnsRef() {
        String url = "https://contestcalendar.com/contestdetails.php?ref=8";

        String result = service.extractWa7bnmRef(url);

        assertThat(result).isEqualTo("8");
    }

    @Test
    void testExtractWa7bnmRef_UrlWithExtraParams_ReturnsRef() {
        String url = "https://contestcalendar.com/contestdetails.php?ref=123&other=value";

        String result = service.extractWa7bnmRef(url);

        assertThat(result).isEqualTo("123");
    }

    @Test
    void testExtractWa7bnmRef_UrlWithRefAfterAmpersand_ReturnsRef() {
        String url = "https://contestcalendar.com/contestdetails.php?other=value&ref=456";

        String result = service.extractWa7bnmRef(url);

        assertThat(result).isEqualTo("456");
    }

    @Test
    void testExtractWa7bnmRef_NullUrl_ReturnsNull() {
        String result = service.extractWa7bnmRef(null);

        assertThat(result).isNull();
    }

    @Test
    void testExtractWa7bnmRef_BlankUrl_ReturnsNull() {
        String result = service.extractWa7bnmRef("   ");

        assertThat(result).isNull();
    }

    @Test
    void testExtractWa7bnmRef_UrlWithoutRef_ReturnsNull() {
        String url = "https://contestcalendar.com/contestdetails.php?other=value";

        String result = service.extractWa7bnmRef(url);

        assertThat(result).isNull();
    }

    @Test
    void testExecuteRefresh_SetsWa7bnmRef() {
        List<ContestICalDto> dtos = List.of(
                new ContestICalDto(
                        "Test Contest",
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        Instant.now().plus(3, ChronoUnit.DAYS),
                        "https://contestcalendar.com/contestdetails.php?ref=42"
                )
        );
        when(contestClient.fetch()).thenReturn(dtos);

        service.executeRefresh();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ContestEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getWa7bnmRef()).isEqualTo("42");
    }

    private List<ContestICalDto> createTestDtos() {
        Instant now = Instant.now();
        return List.of(
                new ContestICalDto(
                        "CQ WW DX Contest",
                        now.plus(1, ChronoUnit.DAYS),
                        now.plus(3, ChronoUnit.DAYS),
                        "https://example.com/cqww"
                ),
                new ContestICalDto(
                        "ARRL 10m Contest",
                        now.plus(7, ChronoUnit.DAYS),
                        now.plus(9, ChronoUnit.DAYS),
                        "https://example.com/arrl10m"
                )
        );
    }
}
