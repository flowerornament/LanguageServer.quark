# LanguageServer.quark

A SuperCollider Language Server Protocol implementation, forked and enhanced for the [Zed SuperCollider extension](https://github.com/flowerornament/zed-supercollider).

## About This Fork

This is an actively maintained fork of [scztt/LanguageServer.quark](https://github.com/scztt/LanguageServer.quark). It serves as the LSP backend for Zed's SuperCollider support, communicating via UDP with a Rust launcher bridge.

## Features

- **Go to Definition** - Jump to class and method definitions
- **Find References** - Find all usages of a symbol
- **Hover** - Documentation on hover (class docs + schelp conversion)
- **Completions** - Class names and methods with snippets
- **Workspace Symbols** - Search classes and methods
- **CodeLens** - Clickable "Evaluate" buttons in the gutter
- **CodeAction** - Quick actions (e.g., open help docs)
- **Folding/Selection Ranges** - Code folding support
- **Execute Commands** - Evaluate code, boot server, stop sounds

## Enhancements Over Original

- **LRU caching** for definition/reference lookups (faster repeated queries)
- **Document rehydration** - handles race conditions when events arrive before didOpen
- **Recompile support** - providers re-register after class library recompile
- **Help docs integration** - schelp → markdown conversion for hover
- **Improved error handling** - standard LSP error codes, lifecycle state tracking
- **Debug logging** - gated behind SCLANG_LSP_DEBUG env var
- **Windows path support** - platform-independent URI handling
- **CodeAction for help** - quick action to open documentation

## Installation

This quark is typically used as part of the [zed-supercollider](https://github.com/flowerornament/zed-supercollider) extension. For standalone use:

```supercollider
Quarks.install("https://github.com/flowerornament/LanguageServer.quark")
```

## Architecture

The quark implements LSP providers that communicate via UDP:

- `LSP.sc` - Main message router and UDP connection
- `LSPDatabase.sc` - Symbol lookup, caching, and utilities
- `LSPDocument.sc` - Open document state management
- `Providers/` - Individual LSP capability implementations

## Related Projects

- [zed-supercollider](https://github.com/flowerornament/zed-supercollider) - Zed extension using this quark
- [sc_launcher](https://github.com/flowerornament/zed-supercollider/tree/main/server/launcher) - Rust bridge between Zed and this LSP

## License

GPL-3.0 (same as SuperCollider)
