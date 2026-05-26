# LiteItemListFabric

A server-side fabric mod enables multiplayer collaborative material preparation.

## Function & Usage

The mod provides commands and gui to create, manage, and operate material preparation task.

### Task management

```
/liteitemlist task <cancel|join|leave|list|new|switch>
```

Or sneak and left click any block with firework rocket in hand to open a gui menu (chest style).

#### Creating task

```
/liteitemlist task new <project_name> <file_name>
```

- `project_name`: It's up to you;
- `file_name`: The litemetica file used for task creation.

LiteItemListFabric will read the targetted litemetica file (Uploaded through syncmetica)

#### Cancelling/Joining/Leaving/Switching task

```
/liteitemlist task <cancel|join|leave|switch> <project_name>
```

- `project_name`: Task to cnacel.

Additional Notes:

- Only the creator or operator can cancel tasks.
- "Switch to a task" means you are currently working on this task, just like switch a channel.

### List view and operate

Open the task material list GUI via **Shift + Left Click** task icon in task panel, or use command:

```
/liteitemlist list
```

While you have a opearting task, you can sneak and left click any block with firework rocket to open task material list GUI directly.

#### Material List Features

- Browse all required materials with total demand, remaining quantity, notes and claimant info;
- **Single click** material icon to claim or unclaim material preparation work;
- **Shift + click** to view detailed item properties;
- Built-in filter to sort materials by claim status, completion status and mark tags;
- Support quantity conversion for in-game unit measurement.

### NPC Dummy Storage Management

The mod provides spawnable storage dummy NPCs for task material storage:

- Enter dummy management GUI via player head icon on material list top bar;
- Create auto-named dummy NPCs bound to specific task;
- Click dummy icon to spawn or teleport dummy to your current position;
- One-click command to summon all task storage dummies;
- View dummy online status, inventory usage and storage capacity in GUI tooltip.

### Multiplayer Communication

Built-in independent task internal chat channel:

- Isolate task-related messages from global chat;
- Avoid irrelevant players being disturbed by mass task messages;
- Real-time message synchronization among all task participants.

## For server Administrators

### Requirements
- Minecraft: 1.21.4
- Fabric Loader: 0.18.4 or higher
- Dependencies: fabric-api, carpet 1.4.161+, syncmatica 0.3.14-sakura.4+

File will save under .\liteitemlist folder.

## Development
### Prerequisites
- Java 21 or higher
- Gradle

### Building from Source
1. Clone the repository
2. Open a terminal in the project directory
3. Run `./gradlew build`
4. The built mod will be in `build/libs/`

## License
This project is licensed under CC0 1.0 license.

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## Contact
For any questions or issues, please open an issue on the GitHub repository.
