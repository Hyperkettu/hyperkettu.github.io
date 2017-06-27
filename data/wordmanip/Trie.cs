using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SearchNode {

	public List<SearchNode> children = new List<SearchNode>();
	public SearchNode parent; 
	public int depth;
	public string word;

}

public class TrieNode {

	public Dictionary<char, TrieNode> children = new Dictionary<char, TrieNode> ();
	public bool isWord = false;
}

public class Trie {

	public TrieNode root;

	public Trie() {
		root = new TrieNode ();

	}

	public void Add(List<string> words) {
		for (int i = 0; i < words.Count; i++) {
			Add (words [i]);
		}
	}

	public void Add(string[] words) {
		for (int i = 0; i < words.Length; i++) {
			Add (words [i]);
		}
	}

	public void Add(string word){

		char[] w = word.ToLower ().ToCharArray ();

		TrieNode current = root;

		for (int i = 0; i < w.Length; i++) {

			char ch = w[i];

			TrieNode newTrie;
			// if current node contains key ch move to the its child
			if (current.children.ContainsKey(ch))
			{
				current = current.children[ch];
			} else {
				// create a new trie node 
				newTrie = new TrieNode();

				if (i == w.Length - 1)
				{
					newTrie.isWord = true;
				}

				current.children.Add(ch, newTrie);
				current = newTrie;
			}

		}

	}

	public bool WordsExist(List<string> words){

		bool wordsExist = true;

		for (int i = 0; i < words.Count; i++) {
			if (!WordExists (words [i])) {
				wordsExist = false;
			}
		}

		return wordsExist;
	}

	public bool WordExists(string word) {

		char[] w = word.ToCharArray ();

		TrieNode current = root;

		for (int i = 0; i < w.Length; i++)
		{
			char ch = w [i];

			if (current.children.ContainsKey(ch))
			{
				current = current.children[ch];

				if (i == w.Length - 1)
				{
					if (current.isWord)
					{
						return true;
					}
				}
			} else {
				return false;
			}
		}
		return false;
	}
}
