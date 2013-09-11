package edu.wwu.cs412.tvfanatic.http;

public interface AsyncTaskCompleteListener<T> {
	public void onTaskComplete(T result);
}
