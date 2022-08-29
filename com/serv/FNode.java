package com.serv;
import java.util.*;

public class FNode {
	int index = -1;
	public String name = "";
	public boolean hasChildren = false;
	public List<FNode> children = new ArrayList<FNode>();

	public FNode(String data){
		name = data;
	}
	
	public FNode(String data, int number){
		name = data;
		index = number;
	}

	public FNode(String data, List<FNode> child){
		name = data;
		children = child;
	}
	
	public void appendFNode(FNode node) {
		children.add(node);
		hasChildren = true;
	}
	
	public void appendStr(String data) {
		children.add(new FNode(data));
		hasChildren = true;
	}
	
	public void appendStr(String data, int number) {
		children.add(new FNode(data, number));
		hasChildren = true;
		
	}
}