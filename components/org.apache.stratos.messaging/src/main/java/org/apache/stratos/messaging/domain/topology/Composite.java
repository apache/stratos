package org.apache.stratos.messaging.domain.topology;

import java.util.List;

public interface Composite {
	
	public void add(Subscribable subscribable);
	
	public void remove(Subscribable subscribable);
	
	public Subscribable getParent();
	
	public void setParent(Subscribable subscribable);
	
}