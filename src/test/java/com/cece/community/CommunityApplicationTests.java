package com.cece.community;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
class CommunityApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	public void testMap(){
		ArrayList<Integer> integers = new ArrayList<>();
		for (int i = 0 ; i < integers.size();i++){
			integers.add(i);
		}
		long start = System.currentTimeMillis();
		ArrayList<Float> floats = new ArrayList<>();
		for(int i : integers){
			floats.add((float)i);
		}
		long end = System.currentTimeMillis();
		System.out.println(end-start);
		start = System.currentTimeMillis();

		List<Float> collect = integers.stream().map(new Function<Integer, Float>() {
			@Override
			public Float apply(Integer integer) {
				return (float) integer;
			}
		}).collect(Collectors.toList());
		end = System.currentTimeMillis();
		System.out.println(end - start);
	}

}
