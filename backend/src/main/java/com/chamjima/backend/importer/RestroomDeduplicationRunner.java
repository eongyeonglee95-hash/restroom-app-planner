package com.chamjima.backend.importer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.chamjima.backend.domain.Restroom;
import com.chamjima.backend.repository.RestroomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 공공데이터 원본이 같은 화장실을 여러 관리번호로 중복 수록하는 문제를 정리한다.
 *
 * <p>원본 CSV 자체에 같은 시설이 2~5번 들어 있고(같은 연도 안에서도, 연도판이 겹쳐서도),
 * external_id는 그때마다 다르게 부여되므로 UNIQUE 제약으로는 걸러지지 않는다. 행을 지우면
 * 거기 달린 리뷰와 원본 추적성을 잃으므로, 대신 대표 행을 정해 나머지 행의 canonical_id가
 * 대표를 가리키게 한다. 조회는 canonical_id가 null인 행만 본다.
 *
 * <p>DB만 읽으므로 CSV 임포트와 독립적으로 언제든 다시 돌릴 수 있다. 임포트로 새 행이
 * 들어온 뒤 한 번 실행하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dedup.restrooms.enabled", havingValue = "true")
public class RestroomDeduplicationRunner implements CommandLineRunner {

	private final RestroomRepository restroomRepository;

	@Override
	public void run(String... args) {
		List<Restroom> all = restroomRepository.findAll();
		Map<String, List<Restroom>> groups = new LinkedHashMap<>();
		for (Restroom restroom : all) {
			String key = groupKey(restroom);
			if (key == null) {
				continue;
			}
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(restroom);
		}

		List<Restroom> changed = new ArrayList<>();
		int duplicateGroups = 0;

		for (List<Restroom> group : groups.values()) {
			// 대표는 가장 작은 내부 id. 재실행해도 같은 행이 뽑혀야 리뷰가 딸려 움직이지 않는다.
			Restroom canonical = group.stream().min(Comparator.comparing(Restroom::getId)).orElseThrow();
			if (group.size() > 1) {
				duplicateGroups++;
			}

			for (Restroom restroom : group) {
				Long expected = restroom.getId().equals(canonical.getId()) ? null : canonical.getId();
				if (!Objects.equals(restroom.getCanonicalId(), expected)) {
					restroom.setCanonicalId(expected);
					changed.add(restroom);
				}
			}
		}

		if (!changed.isEmpty()) {
			restroomRepository.saveAll(changed);
		}

		long hidden = all.stream().filter(r -> r.getCanonicalId() != null).count();
		log.info("화장실 중복 정리 완료: 전체 {}행, 중복 그룹 {}개, 숨김 처리 {}행, 이번에 변경 {}행 (노출 {}행)",
			all.size(), duplicateGroups, hidden, changed.size(), all.size() - hidden);
	}

	/**
	 * 같은 화장실로 볼 기준: 이름 + 좌표.
	 *
	 * <p>주소는 넣지 않는다 — 같은 장소인데 "노량진로 100 (노량진동, 기독교TV멀티미디어센터)"와
	 * "노량진로 100 (노량진동)"처럼 괄호 안 표기만 다른 경우가 41그룹 있어서, 주소를 키에
	 * 넣으면 중복이 안 잡힌다. 반대로 좌표만 쓰면 노량진역(1)/(2)/(3)처럼 좌표가 같은 별개
	 * 화장실까지 합쳐지므로(357그룹) 이름은 반드시 필요하다.
	 */
	static String groupKey(Restroom restroom) {
		if (restroom.getName() == null || restroom.getLatitude() == null || restroom.getLongitude() == null) {
			return null;
		}
		return restroom.getName().trim() + "|" + restroom.getLatitude() + "|" + restroom.getLongitude();
	}
}
