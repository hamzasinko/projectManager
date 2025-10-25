# Gestion de Tâches — Tarnished

## Overview

This repository contains a multi-module Maven project structured into three main modules:

- `tarnished/`
- `tarnished/project-model/`
- `tarnished/project-repomem/`

Each module serves a specific purpose in the application architecture and works together to form a complete task management system.

---

## Project Structure and Responsibilities

### 1. project-model

This module defines the **data models** (entities) used across the application.  
It includes classes such as:

- `Issue`
- `Project`
- `Story`
- (and others as needed)

These classes represent the core business objects and are shared among the other modules.

---

### 2. project-repomem

This module contains the **repository layer** for the models defined in `project-model`.

It includes:

- **Repository interfaces** for each model, providing methods to perform common operations such as:
  - `add`
  - `get`
  - `find`
  - `update`
  - `delete`

- **In-memory implementation classes** (e.g., `IssueMem`, `ProjectMem`, etc.) that implement the repository interfaces.

- A **RepositoryFactory** class that acts as a service layer.  
  It maintains references to the repository interfaces and their in-memory implementations and provides methods to interact with them.  
  This design allows the frontend application to access data through a consistent and centralized service interface.

---

### 3. tarnished

This is the **main application module**.  
It integrates the components from `project-model` and `project-repomem`, providing the entry point for the application.  
This module can also contain the web or API layer if the application exposes endpoints for frontend interaction.

---

## Build Instructions

Before making any changes to the source code in any of the following modules:
- `tarnished`
- `tarnished/project-model`
- `tarnished/project-repomem`

Run the following command from the root of the repository:

```bash
mvn install