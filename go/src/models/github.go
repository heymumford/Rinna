/*
 * GitHub webhook event models for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package models

// GitHubRepository represents a GitHub repository
type GitHubRepository struct {
	ID       int    `json:"id"`
	Name     string `json:"name"`
	FullName string `json:"full_name"`
	HTMLURL  string `json:"html_url"`
	Private  bool   `json:"private"`
}

// GitHubUser represents a GitHub user
type GitHubUser struct {
	Login     string `json:"login"`
	ID        int    `json:"id"`
	AvatarURL string `json:"avatar_url"`
	HTMLURL   string `json:"html_url"`
}

// GitHubPullRequest represents a GitHub pull request
type GitHubPullRequest struct {
	ID        int         `json:"id"`
	Number    int         `json:"number"`
	Title     string      `json:"title"`
	Body      string      `json:"body"`
	State     string      `json:"state"`
	User      GitHubUser  `json:"user"`
	HTMLURL   string      `json:"html_url"`
	Additions int         `json:"additions"`
	Deletions int         `json:"deletions"`
	Labels    []GitHubLabel `json:"labels"`
}

// GitHubLabel represents a GitHub label
type GitHubLabel struct {
	ID    int    `json:"id"`
	Name  string `json:"name"`
	Color string `json:"color"`
}

// GitHubIssue represents a GitHub issue
type GitHubIssue struct {
	ID      int         `json:"id"`
	Number  int         `json:"number"`
	Title   string      `json:"title"`
	Body    string      `json:"body"`
	State   string      `json:"state"`
	User    GitHubUser  `json:"user"`
	HTMLURL string      `json:"html_url"`
	Labels  []GitHubLabel `json:"labels"`
}

// GitHubWorkflowRun represents a GitHub workflow run
type GitHubWorkflowRun struct {
	ID           int    `json:"id"`
	Name         string `json:"name"`
	HeadBranch   string `json:"head_branch"`
	HeadSHA      string `json:"head_sha"`
	Status       string `json:"status"`
	Conclusion   string `json:"conclusion"`
	HTMLURL      string `json:"html_url"`
	JobsURL      string `json:"jobs_url"`
	CheckSuiteID int    `json:"check_suite_id"`
}

// GitHubCommit represents a GitHub commit
type GitHubCommit struct {
	ID        string     `json:"id"`
	Message   string     `json:"message"`
	Timestamp string     `json:"timestamp"`
	Author    GitHubUser `json:"author"`
	URL       string     `json:"url"`
}

// GitHubPusher represents a GitHub pusher
type GitHubPusher struct {
	Name  string `json:"name"`
	Email string `json:"email"`
}

// GitHubPullRequestEvent represents a GitHub pull request event payload
type GitHubPullRequestEvent struct {
	Action      string           `json:"action"`
	Number      int              `json:"number"`
	PullRequest GitHubPullRequest `json:"pull_request"`
	Repository  GitHubRepository `json:"repository"`
	Sender      GitHubUser       `json:"sender"`
}

// GitHubIssuesEvent represents a GitHub issues event payload
type GitHubIssuesEvent struct {
	Action     string           `json:"action"`
	Issue      GitHubIssue      `json:"issue"`
	Repository GitHubRepository `json:"repository"`
	Sender     GitHubUser       `json:"sender"`
}

// GitHubWorkflowRunEvent represents a GitHub workflow run event payload
type GitHubWorkflowRunEvent struct {
	Action      string           `json:"action"`
	WorkflowRun GitHubWorkflowRun `json:"workflow_run"`
	Repository  GitHubRepository `json:"repository"`
	Sender      GitHubUser       `json:"sender"`
}

// GitHubPushEvent represents a GitHub push event payload
type GitHubPushEvent struct {
	Ref        string           `json:"ref"`
	Before     string           `json:"before"`
	After      string           `json:"after"`
	Created    bool             `json:"created"`
	Deleted    bool             `json:"deleted"`
	Forced     bool             `json:"forced"`
	Commits    []GitHubCommit   `json:"commits"`
	HeadCommit GitHubCommit     `json:"head_commit"`
	Repository GitHubRepository `json:"repository"`
	Pusher     GitHubPusher     `json:"pusher"`
	Sender     GitHubUser       `json:"sender"`
}