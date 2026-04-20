Task Management — Tarnished
Overview

This repository contains a multi-module Maven project structured into four main modules:

tarnished/
├─ project-model/
├─ project-repomem/
├─ project-app/
└─ tarnished/


Each module serves a specific purpose in the application architecture and works together to form a complete task management system.

Project Structure and Responsibilities
1️⃣ project-model

This module defines the data models (entities) used across the application.
It includes classes such as:

Issue

Project

Story

(and others as needed)

These classes represent the core business objects and are shared among the other modules.

2️⃣ project-repomem

This module contains the repository layer for the models defined in project-model.
It includes:

Repository interfaces for each model, providing methods to perform common operations such as:

add

get

find

update

delete

In-memory implementation classes (e.g., IssueMem, ProjectMem) that implement the repository interfaces.

RepositoryFactory class that acts as a service layer.

Maintains references to repository interfaces and their in-memory implementations

Provides methods to interact with the repositories consistently

Allows the frontend application to access data through a centralized service interface

3️⃣ project-app

This module contains the web / frontend layer of the application.
It includes:

Controllers for handling HTTP requests

WEB-INF files for views and web resources

Any logic required to expose endpoints to the frontend or client applications

4️⃣ tarnished

This is the main application module.

Integrates components from project-model, project-repomem, and project-app

Provides the entry point of the application

May also contain global configurations or initialization classes

Build Instructions

Before making any changes to the source code in any of the following modules:

tarnished
tarnished/project-model
tarnished/project-repomem
tarnished/project-app


Run the following command from the root of the repository:

mvn install


This will:

Compile all modules

Generate necessary artifacts

Ensure all internal dependencies are resolved