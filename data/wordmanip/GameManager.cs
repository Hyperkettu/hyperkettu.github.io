using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.SceneManagement;

public class GameManager : MonoBehaviour {

	void FindPath(string start, string end, int limit){

		//List<string> next = GenerateNext (start);

		SearchNode root = new SearchNode();

		Search (root, start, end, 0, limit);
		TraverseSearchTree (root, "", end);

		int minDepth = 100;

		for (int i = 0; i < chains.Count; i++) {
			char[] ch = chains [i].ToCharArray ();
			char s = ch[ch.Length - 1];
			string str = "" + s;
			int d = int.Parse (str);
			//int d = int.Parse (new string (ch[ch.Length - 1]));
			if (d < minDepth){
				minDepth = d;
			}
		}

		for (int i = 0; i < chains.Count; i++) {
			if (chains [i].EndsWith ("" + minDepth))
				Debug.Log (chains [i]);
		}

		Debug.Log (start + "->" + end + " " + minDepth);

		SearchNode current = minPath;

		string path = "";

		while (current.parent != null) {
			path = path + current.word + "-";
			current = current.parent;

		}

		path = path + current.word;

		Debug.Log (path);


		/*List<string> next = GenerateNext (start);

		for (int i = 0; i < next.Count; i++) {
			Debug.Log(next[i]);
		}

		Debug.Log (next.Count);*/
	}

	private void Search(SearchNode current, string word, string end, int depth, int limit){

		current.word = word;
		current.depth = depth;

		if (depth >= limit || word == end) {
			if (depth <= minDepth) {
				minDepth = depth;
				minPath = current;
			}
			return;
		}

		List<string> next = GenerateNext (word);

		for (int i = 0; i < next.Count; i++) {

			if (depth >= minDepth) {
				continue;
			}

			SearchNode c = new SearchNode ();
			current.children.Add (c);
			c.parent = current;
			Search (c, next [i], end, depth + 1, limit);
		}

	}

	private List<string> GenerateNext(string word){

		char[] allowed = allowedLetters.ToLower ().ToCharArray ();
		List<string> words = new List<string> ();

		string newWord = word;

		for (int i = 0; i < newWord.Length + 1; i++) {
			for(int j = 0; j < allowed.Length; j++){
				string w = ConstructAddWord (newWord, allowed [j], i); 
				if (dictionary.WordExists (w)) {

					if(!words.Contains(w))
						words.Add (w);
				}
			}
		}

		for (int i = 0; i < newWord.Length; i++) {
			for (int j = 0; j < allowed.Length; j++) {

				if(allowed[j] == newWord[i])
					continue;

				string w = ConstructChangeWord(newWord, allowed[j], i);
				if (dictionary.WordExists (w)) {
					if(!words.Contains(w))
						words.Add (w);
				}
			}
		}

		for (int i = 0; i < newWord.Length; i++) {
			for (int j = 0; j < allowed.Length; j++) {
				string w = ConstructRemoveWord(newWord, i);
				if (dictionary.WordExists (w)) {
					if(!words.Contains(w))
						words.Add (w);
				}
			}
		}

		return words;
	}

	private string ConstructChangeWord(string word, char letter, int index){

		string nextWord = word;

		char[] next = nextWord.ToCharArray ();
		next [index] = letter;
		nextWord = new string (next);
		nextWord = nextWord.ToLower ();
		return nextWord;
	}

	private string ConstructRemoveWord(string word, int index){

		string nextWord = word;
		char[] next = nextWord.ToCharArray ();

		char[] newWord = new char[next.Length - 1];

		for (int i = 0; i < newWord.Length; i++) {
			if (i < index) {
				newWord [i] = nextWord [i];
			} else {
				newWord[i] = nextWord[i+1];
			}
		}

		nextWord = new string (newWord);
		return nextWord;
	}


	private string ConstructAddWord(string word, char letter, int index){

		string nextWord = word;

		char[] next = nextWord.ToCharArray ();

		char[] newWord = new char[nextWord.Length + 1];

		for(int i = 0; i < newWord.Length;i++){
			if (i < index) {
				newWord [i] = nextWord [i];
			} else if (i == index) {
				newWord [i] = letter;
			} else if (i > index) {
				newWord [i] = nextWord [i - 1];
			}
		}
		nextWord = new string (newWord);
		nextWord = nextWord.ToLower ();

		return nextWord;

	}


}
