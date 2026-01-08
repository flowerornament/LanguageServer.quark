/*
 * LRUCache - A simple Least Recently Used cache for SuperCollider.
 * Used by LSPDatabase to cache definition and reference lookups.
 */
LRUCache {
	var <maxSize;
	var cache;      // Dictionary: key -> value
	var recency;    // Array: keys in LRU order (most recent at end)
	var <hits, <misses, <evictions;

	*new { |maxSize = 64|
		^super.new.init(maxSize)
	}

	init { |argMaxSize|
		maxSize = argMaxSize;
		cache = Dictionary.new;
		recency = Array.new(maxSize);
		hits = 0;
		misses = 0;
		evictions = 0;
	}

	size { ^cache.size }

	hitRate {
		var total = hits + misses;
		if (total == 0) { ^0.0 };
		^(hits / total)
	}

	at { |key|
		var value = cache[key];
		if (value.notNil) {
			// Cache hit - promote to most recent
			recency.remove(key);
			recency = recency.add(key);
			hits = hits + 1;
			^value
		};
		misses = misses + 1;
		^nil
	}

	put { |key, value|
		// If key exists, update and promote
		if (cache[key].notNil) {
			recency.remove(key);
		} {
			// New entry - evict if at capacity
			if (cache.size >= maxSize) {
				this.prEvictLRU;
			}
		};
		cache[key] = value;
		recency = recency.add(key);
	}

	prEvictLRU {
		var lruKey;
		if (recency.size > 0) {
			lruKey = recency.removeAt(0);  // Remove oldest (front)
			cache.removeAt(lruKey);
			evictions = evictions + 1;
		}
	}

	clear {
		cache = Dictionary.new;
		recency = Array.new(maxSize);
	}

	includes { |key|
		^cache[key].notNil
	}

	stats {
		^(
			size: this.size,
			maxSize: maxSize,
			hits: hits,
			misses: misses,
			evictions: evictions,
			hitRate: this.hitRate
		)
	}
}
