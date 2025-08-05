# Copilot Instructions

<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

This is a Minecraft server queue management system project. The project includes:

1. **BungeeCord/Velocity Proxy Configuration** - Network proxy for managing multiple Minecraft servers
2. **Queue Management Plugin** - Custom Java plugin for handling player queues
3. **Non-premium Player Support** - Configuration for offline-mode servers
4. **VIP Priority System** - Premium queue management for VIP players
5. **Server Monitoring Tools** - Scripts and utilities for server health monitoring

## Development Guidelines:

- Use Java 17+ for plugin development
- Follow Minecraft plugin development best practices
- Implement proper error handling and logging
- Support both premium and non-premium (cracked) players
- Ensure thread-safe queue operations
- Include comprehensive configuration options
- Provide detailed documentation and setup guides

## Key Technologies:
- Java (Spigot/Paper Plugin API)
- Velocity/BungeeCord Proxy
- MySQL/SQLite for data persistence
- YAML configuration files
- Maven for dependency management

When generating code, prioritize:
- Performance and scalability
- Security considerations for non-premium support
- User experience in queue management
- Administrative tools and monitoring
- Clear documentation and comments
