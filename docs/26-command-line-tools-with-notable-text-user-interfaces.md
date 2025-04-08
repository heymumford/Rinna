# 26 Command-Line Tools with Notable Text User Interfaces (TUIs)
Copyright(c) 2025 Eric C. Mumford (@heymumford)

As of April 8, 2025, the command line remains a powerful environment, enhanced by numerous tools that offer sophisticated Text User Interfaces (TUIs). These interfaces provide well-structured information, effective use of color, intuitive navigation, and real-time updates, streamlining interaction without needing a full graphical desktop. Here are 26 examples notable for their TUI design:

### 1. htop

An interactive process viewer, significantly enhancing the classic `top`.

* **TUI Features & Scope:**
    * Displays a list of running processes in a scrollable, sortable table.
    * Uses color effectively to highlight CPU/memory usage intensity and process states.
    * Presents CPU (per core), memory, and swap usage via clear, real-time bar graphs.
    * Allows interactive sorting (by CPU%, MEM%, PID, etc.) via function keys or mouse clicks.
    * Supports process searching, filtering (by user), and tree view (showing parent/child relationships).
    * Enables sending signals (e.g., KILL, TERM) to processes directly from the interface using keyboard shortcuts.
    * Highly configurable display (add/remove/reorder columns) via an interactive setup menu.

### 2. btop++ / bpytop

Modern, visually rich resource monitors (C++ and Python versions).

* **TUI Features & Scope:**
    * Presents a comprehensive dashboard layout with distinct sections for CPU, memory, disks, network, and processes.
    * Features smooth, real-time graphs showing historical usage data for CPU (overall and per-core), memory, network I/O, and disk I/O.
    * Includes temperature sensor readings (if available).
    * Process list is sortable, filterable, and displays detailed information (CPU%, Mem%, disk R/W).
    * Offers full mouse support for navigation, sorting, and process selection/killing.
    * Highly customizable via in-application menus for themes, layout options, displayed elements, and update intervals.

### 3. bottom (btm)

A graphical process and system monitor written in Rust.

* **TUI Features & Scope:**
    * Utilizes a widget-based layout that is highly customizable by the user.
    * Displays detailed CPU usage (overall, per-core) with graphical bars or graphs.
    * Shows memory/swap usage, network I/O statistics, disk I/O, and temperature sensors graphically.
    * Includes an interactive process list widget for sorting, searching, and viewing process details.
    * Supports keyboard navigation between widgets and interaction within them.
    * Offers multiple display modes and themes for visual customization.

### 4. ranger

A console file manager heavily inspired by `vi` keybindings.

* **TUI Features & Scope:**
    * Employs a "Miller columns" layout: parent directory, current directory, file/subdirectory preview pane.
    * Provides immediate visual feedback on directory structure and navigation path.
    * Uses color coding to differentiate file types.
    * Integrates preview capabilities for text files, archives, PDFs, and images (requires external tools like `w3mimgdisplay`, `ueberzug`, or similar terminal capabilities).
    * Supports tabs for managing multiple directory views.
    * Operates primarily via `vi`-like keyboard shortcuts for navigation, selection, and file operations.
    * Allows executing shell commands directly via a `:` command prompt.

### 5. Midnight Commander (mc)

A classic twin-panel visual file shell.

* **TUI Features & Scope:**
    * Features a two-panel layout showing directory listings side-by-side, ideal for copy/move operations.
    * Displays function key shortcuts (Copy, Move, Delete, etc.) persistently at the bottom for easy reference.
    * Includes a built-in file viewer (`mcview`) and text editor (`mcedit`) with syntax highlighting.
    * Supports mouse interaction for navigating panels, selecting files, and activating function keys.
    * Can interact with virtual filesystems (e.g., FTP, SFTP, archives) directly within the TUI panels.

### 6. nnn (Nnn's Not Noice)

An extremely fast, lightweight, and minimalist file browser.

* **TUI Features & Scope:**
    * Presents a very clean, fast-rendering single-pane view of the current directory.
    * Uses minimal screen real estate, focusing on speed and responsiveness.
    * Features 4 "contexts" (like workspaces) for quick navigation between directories.
    * Integrates tightly with shell and external tools; TUI facilitates selecting files for batch operations.
    * Displays essential file information in a status bar.
    * Navigation is primarily keyboard-driven, optimized for speed.

### 7. lf (List Files)

A terminal file manager inspired by `ranger`, written in Go.

* **TUI Features & Scope:**
    * Uses a `ranger`-like multi-column layout for intuitive hierarchical navigation.
    * Provides fast, asynchronous previews for files and directories.
    * Built on a client/server model, allowing multiple instances to share resources and state efficiently.
    * Features a clean status bar and uses `vi`-like keybindings for navigation and file operations.
    * Focuses on simplicity and performance within its TUI.

### 8. lazygit

A simple terminal UI designed specifically for streamlining Git operations.

* **TUI Features & Scope:**
    * Multi-pane layout clearly separating Git status, files (staged/unstaged), branches, commits/log, and stash.
    * Enables most common Git actions (stage, unstage, commit, amend, rebase, merge, push, pull, checkout) via single keystrokes, indicated clearly in the UI.
    * Visualizes branches and commit history graphically.
    * Provides an interactive interface for staging specific lines/hunks within files.
    * Offers an intuitive TUI for performing interactive rebases.

### 9. tig (Text-mode Interface for Git)

An ncurses-based text-mode interface primarily for Browse Git repositories.

* **TUI Features & Scope:**
    * Provides multiple views: main (log), diff, tree, blob, blame, refs, status, stage.
    * The main view offers an interactive, scrollable `git log` output.
    * The stage view provides an interactive interface similar to `git add -p` and `git status`.
    * Allows easy navigation between related objects (e.g., jump from a commit in the log to its diff view).
    * Highly navigable via keyboard shortcuts (inspired by `vi` and `less`).
    * Customizable keybindings and appearance.

### 10. Neovim (nvim)

A modern, highly extensible Vim-based text editor.

* **TUI Features & Scope:**
    * Supports advanced TUI features like floating windows for diagnostics (LSP), code completion suggestions, documentation popups, and plugin interfaces.
    * Can render GUI-like elements (menus, popups) within the terminal.
    * Leverages modern terminal features for better color support (24-bit color) and effects.
    * Integrates seamlessly with external tools and language servers, displaying information directly within the editor's TUI.
    * Features an embedded terminal emulator within Neovim windows/splits.
    * Highly customizable status lines (via plugins like lualine, airline) provide rich contextual information.

### 11. micro

A modern terminal-based text editor focused on ease of use and familiar keybindings.

* **TUI Features & Scope:**
    * Uses common Ctrl-key combinations (Ctrl-S save, Ctrl-Q quit, Ctrl-F find) familiar to users of graphical editors.
    * Provides excellent mouse support for positioning the cursor, selecting text, and scrolling.
    * Features a clean status bar showing position, file type, notifications, and keybinding hints.
    * Includes an interactive command bar (Ctrl-E) for executing editor commands.
    * Supports syntax highlighting for numerous languages out-of-the-box.
    * Offers multi-cursor support for efficient batch edits.

### 12. mtr (My Traceroute)

A network diagnostic tool combining `ping` and `traceroute`.

* **TUI Features & Scope:**
    * Displays a continuously updating list of network hops from the source to the destination host.
    * For each hop, shows real-time statistics: packet loss (%), latency (min, avg, max, std dev).
    * The TUI structure makes it easy to visually identify the location of network latency or packet loss along the path.
    * Allows interactive pausing and changing display modes (e.g., show hostnames) via keyboard shortcuts.

### 13. gping

A tool that pings hosts and graphs the latency.

* **TUI Features & Scope:**
    * Provides a simple, focused TUI displaying a real-time graph of ping latency over time.
    * Clearly visualizes network stability, jitter, and outages.
    * Shows summary statistics (min, max, avg latency) alongside the graph.
    * Uses color to enhance readability.

### 14. termshark

A terminal user interface for `tshark` (terminal Wireshark).

* **TUI Features & Scope:**
    * Mimics the classic Wireshark layout with panes for: Packet List, Packet Details (tree view), and Packet Bytes (hex dump).
    * Applies Wireshark's coloring rules to the packet list for easy protocol identification.
    * Allows entering Wireshark display filters interactively to sift through captured packets.
    * Supports keyboard navigation for scrolling through packets and examining details.
    * Can read packet capture files or listen live on interfaces (requires appropriate permissions).

### 15. cmus (C* Music Player)

A small, fast, and powerful console music player.

* **TUI Features & Scope:**
    * Organized into distinct views (Library, Playlist, Queue, File Browser, Settings) accessed via number keys (1-7).
    * Provides efficient keyboard navigation (`vi`-like bindings) within views for Browse artists, albums, tracks.
    * Features a powerful filtering/search mechanism accessible directly within the library/playlist views.
    * Displays playback status, track information, and time elapsed/remaining in a status bar.

### 16. ncmpcpp (NCurses Music Player Client Plus Plus)

A feature-rich MPD (Music Player Daemon) client with a sophisticated TUI.

* **TUI Features & Scope:**
    * Highly customizable interface layout, often featuring columns for artist, album, and track lists.
    * Includes a built-in tag editor TUI for modifying music metadata.
    * Provides interfaces for managing playlists, Browse the music database, and searching.
    * Can display lyrics and features an optional audio visualizer (frequency spectrum).
    * Supports themes for extensive visual customization.

### 17. WeeChat (Wee Enhanced Environment for Chat)

An extensible, multi-protocol chat client.

* **TUI Features & Scope:**
    * Manages multiple chat buffers (servers, channels, private messages) within a single TUI window.
    * Typically displays a nicklist sidebar for the current channel.
    * Features a highly customizable status bar showing connection status, activity notifications, and other info.
    * Supports advanced TUI features like window splitting, mouse support (optional), and scriptable interfaces.
    * Uses color effectively for nicknames, highlighting, and status indicators.

### 18. calcurse

An interactive calendar and scheduling application for the command line.

* **TUI Features & Scope:**
    * Presents a three-pane layout: monthly Calendar view, Appointments list for the selected day, and a TODO list.
    * Allows interactive navigation through dates using keyboard shortcuts.
    * Clearly highlights days with scheduled appointments in the calendar view.
    * Provides interactive prompts and forms for adding/editing appointments and TODO items.
    * Includes a notification system for upcoming events.

### 19. vit (Visual Interactive Taskwarrior)

An ncurses frontend providing a TUI for the Taskwarrior task management system.

* **TUI Features & Scope:**
    * Displays Taskwarrior reports (e.g., `task next`, `task list`) in an interactive, scrollable list.
    * Allows filtering and sorting tasks using Taskwarrior's filter syntax directly within the TUI.
    * Enables performing common task actions (start, stop, done, delete, annotate, modify) via keyboard shortcuts on selected tasks.
    * Supports selecting multiple tasks for batch operations.
    * Provides detailed views for individual tasks.

### 20. ncdu (NCurses Disk Usage)

An interactive ncurses-based disk usage analyzer.

* **TUI Features & Scope:**
    * Scans a directory and presents its contents sorted by size in an interactive list.
    * Uses a simple bar graph (`#` characters) to visually represent relative item sizes.
    * Allows navigating into subdirectories to explore disk usage hierarchically.
    * Provides keyboard shortcuts for sorting (by size, name), refreshing, toggling counts vs. size, and deleting selected files/directories (with confirmation).

### 21. duf (Disk Usage/Free Utility)

A utility to display disk usage/free space with a focus on readable output.

* **TUI Features & Scope:**
    * Presents information about mounted filesystems in a clean, well-formatted table.
    * Uses color-coding (e.g., red/yellow/green) to indicate filesystem usage levels at a glance.
    * Clearly displays mount point, total size, used space, available space, capacity percentage, filesystem type, and optionally inode usage.
    * While primarily non-interactive, its TUI excels at presenting `df` information beautifully.

### 22. mycli

A terminal client for MySQL/MariaDB with enhanced TUI features.

* **TUI Features & Scope:**
    * Provides intelligent, context-aware auto-completion for SQL keywords, functions, table names, column names, and database names via interactive popups.
    * Features syntax highlighting for SQL queries entered at the prompt, improving readability.
    * Supports multi-line query editing.
    * Offers various output formats for query results (table, vertical, csv, etc.), improving on the default client's presentation.

### 23. pgcli

A terminal client for PostgreSQL with enhanced TUI features.

* **TUI Features & Scope:**
    * Similar to `mycli`, offers smart auto-completion for PostgreSQL-specific keywords, functions, schemas, tables, columns, views, etc., via interactive popups.
    * Provides syntax highlighting tailored to PostgreSQL's SQL dialect.
    * Supports multi-line editing and displays query results in well-formatted tables or other formats.
    * Includes shortcuts for common meta-commands (`\d`, `\l`, etc.) with completion support.

### 24. aptitude

An Ncurses frontend for the APT package manager (Debian/Ubuntu).

* **TUI Features & Scope:**
    * Presents package lists in a hierarchical, navigable tree (e.g., by status, section, task).
    * Displays detailed information for selected packages, including description, dependencies, reverse dependencies, and version information.
    * Uses clear visual indicators (colors, symbols) for package states (installed, upgradable, broken, new, to be installed/removed).
    * Provides an interactive interface for selecting packages for installation, removal, or upgrade, including handling dependency resolution interactively.
    * Features powerful search capabilities within the TUI.

### 25. tmux

A powerful terminal multiplexer.

* **TUI Features & Scope:**
    * Provides session, window, and pane management entirely within the terminal.
    * Features a highly customizable status bar at the bottom displaying active windows, pane titles, time, system load, or custom script outputs.
    * Visually delineates panes with borders and highlights the active pane.
    * Offers a "copy mode" TUI for scrolling back through terminal history, selecting text, and copying it to a tmux buffer, all via keyboard.
    * Presents lists of sessions, windows, and panes for interactive management (`choose-tree`).

### 26. Emacs

A highly extensible text editor and integrated development environment, fully usable in TUI mode.

* **TUI Features & Scope:**
    * Provides a complete TUI experience, capable of running entirely within a terminal (`emacs -nw`).
    * Features powerful text editing capabilities with syntax highlighting, auto-completion (via various packages), and extensive keybindings.
    * Supports splitting the TUI display into multiple windows (panes) and managing multiple frames (like OS windows).
    * Includes built-in TUI applications like `dired` (a powerful file manager) and interfaces for email, newsgroups, and more.
    * Hosts exceptional TUI packages like `Magit` (a widely acclaimed Git interface) and `Org mode` (for notes, planning, literate programming with dedicated TUI views like the agenda).
    * The "mode line" acts as a dynamic status bar, showing buffer status, position, active modes, etc.
    * Infinitely customizable appearance (colors, themes) and behavior through Elisp. Emacs is less a single tool and more a TUI *platform*.

