package tesrac.utils;

/**
 * This class represents a pair of elements
 * @author JBecho
 *
 * @param <K> First Object
 * @param <V> Second Object
 */
public class Pair<K, V> {

	/*
	 * The first element
	 */
	private K first;
	
	/*
	 * The second element
	 */
	private V second;
	
	/**
	 * Constructor
	 * @param first
	 * @param second
	 */
	public Pair(K first, V second) {
		this.first = first;
		this.second = second;
	}
	
	/**
	 * Returns the first element
	 * @return K
	 */
	public K getFirst() {
		return first;
	}
	
	/**
	 * Returns the second element
	 * @return V
	 */
	public V getSecond() {
		return second;
	}
}
