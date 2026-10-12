# AI Agent Development Governance Policy

This policy is mandatory for every AI coding agent working on this project, including Claude Code, OpenAI Codex, GitHub Copilot, Cursor, Gemini, and any other AI coding assistant.

## 1. Mandatory Agent Instructions

All AI agents MUST read and follow `AGENTS.md` before performing any development task.

`AGENTS.md` is the single source of truth for AI development rules.

- Read `AGENTS.md` before starting any task.
- Follow its instructions throughout development.
- Do not bypass or reinterpret its restrictions.
- Re-read relevant instructions when changing modules.
- If instructions conflict, prioritize explicit user instructions and mandatory safety requirements.
- Never silently ignore a rule.

Configure existing agent-specific instruction files, where supported, to reference `AGENTS.md`. For Claude Code, ensure `CLAUDE.md` directs Claude to follow `AGENTS.md`. Do not duplicate the entire policy across multiple instruction files.

## 2. Strict No Automated Test Generation Policy

AI agents MUST NOT independently create automated tests.

This includes:

- Unit tests
- Integration tests
- End-to-end tests
- UI automation tests
- Snapshot tests
- Performance tests
- Test fixtures
- Mock test implementations
- Test scripts
- Automated testing infrastructure

Rules:

1. Do not generate tests unless explicitly instructed by the user.
2. Do not create test directories independently.
3. Do not install testing frameworks without authorization.
4. Do not introduce testing dependencies without authorization.
5. Do not create test-related CI/CD workflows.
6. Do not generate tests as part of refactoring.
7. Do not automatically create tests when fixing bugs.
8. Do not execute automated tests unless explicitly requested.

If the agent created unauthorized tests during its current task, it MUST delete those files.

Never delete pre-existing tests or user-authored tests without explicit authorization.

Agents may inspect code, perform static reasoning, and use non-test validation such as syntax checks and compilation, provided those actions remain within the requested task.

## 3. Documentation Policy

AI agents MUST NOT create documentation unless explicitly requested.

Prohibited without authorization:

- README files
- API documentation
- Architecture documentation
- Implementation guides
- Migration guides
- Technical reports
- Markdown documentation files
- Automatically generated documentation
- Unrequested changelogs

Exceptions:

- `AGENTS.md` and minimal agent instruction references required by this task.
- Required inline code comments.
- Documentation explicitly requested by the user.

Do not modify existing documentation unless the task requires it.

## 4. Strict Task Scope Enforcement

AI agents MUST perform ONLY the task explicitly requested.

The agent MUST NOT:

- Add unrelated features.
- Modify unrelated modules.
- Refactor unrelated code.
- Change architecture unnecessarily.
- Introduce additional dependencies without justification.
- Modify UI components outside the requested scope.
- Rename unrelated classes, functions, or files.
- Introduce speculative improvements.
- Implement future requirements without authorization.
- Modify deployment configuration without authorization.
- Perform unrelated cleanup.

If the agent identifies unrelated problems:

1. Do not fix them automatically.
2. Report them separately.
3. Continue with the original requested task.
4. Wait for authorization before making unrelated changes.

The agent must never expand the scope of a task independently.

## 5. No Overengineering

All implementation must prioritize simplicity, maintainability, and minimal complexity.

Engineering principles:

- KISS: Keep It Simple.
- YAGNI: You Aren't Gonna Need It.
- DRY: Don't Repeat Yourself, when reuse genuinely improves clarity.
- Prefer existing implementations.
- Reuse existing components.
- Follow established architecture.
- Avoid unnecessary abstractions.
- Avoid excessive design patterns.
- Avoid speculative scalability implementations.
- Avoid creating generic frameworks for one-time functionality.
- Avoid introducing new layers without a concrete requirement.

Do not create:

- Unnecessary interfaces.
- Unnecessary base classes.
- Unnecessary factories.
- Unnecessary wrappers.
- Unnecessary services.
- Unnecessary repositories.
- Unnecessary configuration systems.
- Unnecessary background workers.
- Unnecessary microservices.

Every abstraction must solve an actual requirement.

Prefer the smallest correct implementation that satisfies the requested behavior.

## 6. Mandatory Code Commenting

Every new or modified function, class, and logically independent code snippet MUST include a concise explanatory comment.

Comment requirements:

- Minimum: 1 line.
- Maximum: 5 lines.
- Explain responsibility and meaningful behavior.
- Document important assumptions where relevant.
- Explain non-obvious logic.
- Explain lifecycle responsibilities when applicable.

Rules:

1. Use the programming language's conventional comment format.
2. Prefer documentation comments for public classes and functions.
3. Keep comments accurate when implementation changes.
4. Do not write unnecessarily verbose comments.
5. Avoid comments that merely repeat the code.
6. Do not add comments to unrelated code solely for compliance.
7. Do not use comments as a replacement for readable naming.

Example:

```text
// Validates authentication input and returns a structured result.
// Rejects invalid credentials without exposing sensitive details.
```

For complex functions, use up to five meaningful lines.

For short functions, one concise line is sufficient.

## 7. Complete Code Lifecycle Responsibility

Every AI agent is responsible for the complete lifecycle of the code it creates or modifies.

Do not deliver incomplete or disconnected implementations.

Every implementation must consider, where applicable:

1. Initialization
2. Configuration
3. Input validation
4. Authentication and authorization
5. Execution
6. State management
7. Error handling
8. Resource management
9. Cleanup
10. Shutdown or disposal
11. Failure recovery
12. Compatibility with existing functionality

Examples:

Database:

- Connections must be properly managed.
- Transactions must commit or roll back.
- Resources must be released.
- Partial failures must be handled.

Network:

- Connections must close appropriately.
- Timeouts must be handled.
- Failures must not leave invalid state.

Background operations:

- Tasks must have an explicit start and stop lifecycle.
- Cancellation must be supported where relevant.
- Resources must be cleaned up.

UI:

- Loading, success, error, and empty states must be handled when relevant to the requested feature.
- Event listeners must be removed when no longer needed.
- Components must release owned resources.

APIs:

- Validate requests.
- Return appropriate responses.
- Handle expected failure conditions.
- Preserve existing API contracts.

Complete lifecycle responsibility does not authorize additional features or unrelated architecture changes.

If a required lifecycle component is outside the authorized scope, report the dependency rather than silently expanding the implementation.

## 8. Production-Safe Migration Policy

All migrations are considered HIGH-RISK operations.

This applies to:

- Database migrations
- API migrations
- Authentication migrations
- Infrastructure migrations
- Storage migrations
- Schema changes
- Data transformations
- Service replacements
- Configuration migrations

Agents MUST prioritize production continuity.

### General Migration Rules

1. Never assume production data can be deleted.
2. Never introduce breaking changes without an approved plan.
3. Preserve existing functionality during migrations.
4. Prefer backward-compatible changes.
5. Avoid destructive schema modifications.
6. Preserve existing integrations.
7. Ensure rollback or recovery is possible.
8. Never execute production migrations without explicit approval.
9. Never silently change production credentials or configuration.
10. Never remove legacy behavior before its approved retirement.

### Database Migrations

Use an expand-migrate-contract strategy where applicable.

Expand:

- Add backward-compatible schema changes.
- Preserve existing columns and tables.
- Avoid breaking existing queries.

Migrate:

- Migrate existing data safely.
- Handle partial failures.
- Preserve data integrity.
- Use controlled batching for large datasets where appropriate.

Contract:

- Remove obsolete structures only after confirming that dependent systems no longer require them.
- Require explicit authorization for destructive changes.

Never automatically delete existing production data.

## 9. Strict API Versioning Policy

Existing production API contracts MUST remain backward-compatible.

Example existing endpoint:

```text
/api/v1/login
```

If a breaking change is required, create a new version:

```text
/api/v2/login
```

Do not silently replace or modify the behavior of `/api/v1/login`.

API versioning rules:

1. Follow `/api/v{versionNumber}/resource` for versioned APIs.
2. Preserve existing API versions during migration.
3. Create a new API version for breaking contract changes.
4. Avoid unnecessary API version increases for compatible changes.
5. Maintain compatibility with existing consumers.
6. Define an explicit deprecation date.
7. Define an explicit retirement date.
8. Communicate deprecation through the existing API response metadata or supported headers.
9. Never remove an API immediately after introducing its successor.
10. Require an approved migration plan before retiring an API.

Example migration lifecycle:

Phase 1:

- Deploy `/api/v2/login`.
- Keep `/api/v1/login` operational.

Phase 2:

- Mark `/api/v1/login` as deprecated.
- Announce its retirement date.
- Allow clients to migrate.

Phase 3:

- Monitor remaining usage.
- Confirm compatibility and migration readiness.

Phase 4:

- Retire `/api/v1/login` on its approved retirement date, unless an authorized Super Admin has placed a hold.

Phase 5:

- Preserve appropriate retirement and audit records.

## 10. Automated API Deprecation Management

The application must support controlled API retirement when API lifecycle management is explicitly implemented.

Every deprecated API version should have lifecycle metadata:

- API version
- Endpoint identifier
- Deprecation date
- Scheduled retirement date
- Lifecycle status
- Super Admin retirement hold
- Replacement endpoint
- Last lifecycle update

Recommended lifecycle states:

- `ACTIVE`
- `DEPRECATED`
- `RETIREMENT_SCHEDULED`
- `RETIRED`

Expected behavior:

`ACTIVE`:

- Endpoint remains operational.

`DEPRECATED`:

- Endpoint remains operational.
- Clients receive appropriate deprecation information.

`RETIREMENT_SCHEDULED`:

- Endpoint remains operational until the approved retirement date unless an earlier approved policy applies.

`RETIRED`:

- Endpoint no longer processes the legacy operation.
- Return HTTP 410 Gone when appropriate.
- Do not silently redirect requests with incompatible semantics.

### Automatic Retirement

Once the approved retirement date is reached, the system should automatically disable the deprecated endpoint unless a Super Admin retirement hold is active.

### Super Admin Control

A Super Admin must be able to:

- View API lifecycle status.
- Place a hold on scheduled retirement.
- Remove an existing retirement hold.
- Approve changes to retirement dates.
- Review lifecycle changes.

Security requirements:

- Enforce authorization server-side.
- Never rely solely on frontend role checks.
- Record sensitive lifecycle administration actions.
- Prevent unauthorized users from overriding retirement policy.

This section defines the policy for future API lifecycle functionality.

Do NOT implement an API lifecycle management system, database tables, scheduled jobs, or Super Admin UI unless explicitly requested.

## 11. Change Safety

Before modifying code:

- Understand the relevant existing implementation.
- Identify affected modules.
- Identify potential compatibility issues.
- Check existing architectural conventions.
- Determine the smallest safe change.

During implementation:

- Preserve existing behavior unless modification is requested.
- Do not introduce unrelated changes.
- Handle relevant failure scenarios.
- Follow established naming conventions.
- Avoid adding unnecessary dependencies.

After implementation:

- Review the changes made.
- Check for unintended modifications.
- Confirm required lifecycle handling.
- Perform permitted non-test validation.
- Do not create automated tests.
- Do not generate documentation.
- Do not perform unrelated cleanup.

## 12. Agent Completion Requirements

At the end of every development task, provide a concise summary.

The response must contain:

1. What was implemented.
2. Which files were modified.
3. Whether the requested functionality is complete.
4. Any important limitations or risks.
5. Whether production migration or approval is required.

Do not generate a documentation file for this summary.

Do not claim functionality is verified unless validation was actually performed.

Do not claim production safety without supporting evidence.

## 13. Strict Prohibitions

Unless explicitly requested, agents MUST NOT:

- Generate automated tests.
- Generate documentation.
- Add unrelated functionality.
- Perform speculative refactoring.
- Introduce unnecessary architecture.
- Modify production infrastructure.
- Execute production migrations.
- Delete production data.
- Break existing API contracts.
- Remove existing endpoints.
- Modify authentication behavior outside the task.
- Introduce unnecessary dependencies.
- Create incomplete lifecycle implementations.
- Change unrelated code.

## 14. Final Task Instructions

When establishing or updating this governance policy:

1. Inspect the repository root.
2. Check whether `AGENTS.md` already exists.
3. If it exists, preserve relevant project-specific instructions and integrate governance rules without losing them.
4. If it does not exist, create `AGENTS.md`.
5. Check whether `CLAUDE.md` exists.
6. Configure `CLAUDE.md` to reference `AGENTS.md`.
7. Preserve existing valid `CLAUDE.md` instructions.
8. Avoid creating unnecessary agent-specific configuration files.
9. Do not modify application code.
10. Do not create tests.
11. Do not modify existing tests.
12. Do not create unrelated documentation.
13. Review the final changes.
14. Report the files created or modified.

This governance task is limited to establishing agent governance.

Do NOT start implementing any application features, migration systems, API lifecycle infrastructure, or Super Admin controls described in this policy.

## 15. Mandatory Design System Compliance

All AI agents MUST follow the project's existing design system.

The design system is the SINGLE SOURCE OF TRUTH for all visual design, UI implementation, and frontend development.

This requirement is mandatory for every AI agent.

### 15.1 Design System Discovery

Before modifying any UI or frontend code, agents MUST:

1. Locate and read the existing design system.
2. Identify existing design tokens.
3. Review available components and established patterns.
4. Understand typography, colors, spacing, and layouts.
5. Understand responsive behavior.
6. Check light and dark theme requirements.
7. Identify accessibility conventions.

If a design system exists, it MUST be followed.

If no design system exists, follow established UI conventions. Do not create a new design system without authorization.

### 15.2 Strict Design Token Usage

Agents MUST use existing design tokens.

This applies to:

- Colors
- Typography
- Spacing
- Border radius
- Borders
- Backgrounds
- Component states
- Responsive layouts
- Shadows, if supported
- Animations, if supported

Agents MUST NOT:

- Introduce arbitrary colors.
- Hardcode design values unnecessarily.
- Create duplicate design tokens.
- Invent new typography scales.
- Introduce inconsistent spacing.
- Create custom component styles unnecessarily.
- Override existing design tokens without authorization.

For ACS projects, follow the existing ACS Design System in `docs/DESIGN_SYSTEM.md`.

Examples:

- Primary brand: `#00875A`
- Primary brand hover: `#006B48`
- Primary brand pressed: `#005238`
- Primary background: `#FFFFFF`
- Dark background: `#0A0A0A`

Use the actual design tokens defined by the project rather than hardcoding these values.

### 15.3 Component Reusability

Before creating a new UI component:

1. Search for an existing component.
2. Determine whether it satisfies the requirement.
3. Reuse it whenever possible.
4. Extend an existing component only when necessary.
5. Create a new component only when no suitable component exists.

Agents MUST NOT independently introduce:

- Duplicate buttons
- Duplicate input fields
- Duplicate cards
- Duplicate navigation components
- Duplicate dialogs
- Duplicate typography systems
- Duplicate layout primitives

### 15.4 Visual Consistency

All new UI implementations MUST maintain consistency with the existing application.

This includes:

- Visual hierarchy
- Component dimensions
- Typography
- Colors
- Alignment
- Padding and margins
- Iconography
- Interaction states
- Navigation patterns
- Responsive layouts

Agents MUST NOT redesign existing interfaces unless explicitly requested.

### 15.5 Responsive Design

All UI changes MUST support the breakpoints defined by the project's design system.

For ACS:

- Mobile: 320–767px
- Tablet: 768–1023px
- Desktop: 1024–1439px
- Large desktop: 1440px+

Avoid horizontal overflow and broken layouts.

Do not introduce additional breakpoints unless required.

### 15.6 Light and Dark Themes

If the application supports multiple themes:

- Follow existing semantic theme tokens.
- Maintain correct contrast.
- Preserve theme-switching behavior.
- Do not hardcode colors that break theme compatibility.
- Do not introduce separate theme systems.

### 15.7 Accessibility

All UI implementations MUST follow the project's established accessibility requirements.

For ACS, target WCAG 2.2 AA.

Requirements include:

- Accessible color contrast.
- Semantic HTML.
- Keyboard navigation.
- Visible focus indicators.
- Accessible component states.
- Appropriate interaction target sizes.
- Reduced-motion support where relevant.

### 15.8 No Unauthorized Design Changes

Agents MUST NOT:

- Redesign pages without authorization.
- Introduce new brand colors.
- Change existing typography.
- Replace the component library.
- Introduce new UI frameworks.
- Modify global styles unnecessarily.
- Add decorative elements without requirements.
- Create new design patterns without justification.
- Change the visual identity independently.

### 15.9 Design System Conflicts

If a requested implementation conflicts with the existing design system:

1. Identify the conflict.
2. Explain the affected design rules.
3. Ask for authorization if an exception is necessary.
4. Do not silently modify the design system.

An explicit user-approved design change may override an existing design rule for the requested scope.

### 15.10 Final Design Compliance Review

Before completing a UI task, agents MUST verify:

- Existing components were reused where appropriate.
- Design tokens were followed.
- Typography remains consistent.
- Spacing and alignment follow the design system.
- Responsive requirements are addressed.
- Theme compatibility is preserved.
- Accessibility requirements are respected.
- No unnecessary visual changes were introduced.

This review does not authorize generating automated tests.

## Governance Maintenance Boundaries

When updating this policy:

1. Preserve Rules 1–15 and existing project-specific instructions unless the user explicitly requests a policy change.
2. Ensure `CLAUDE.md` references `AGENTS.md`.
3. Do not modify application code.
4. Do not create automated tests.
5. Do not create additional documentation.
6. Do not modify the design system itself.
7. Report the changes made.

This policy governs AI agent development only. Do not implement UI changes or refactor existing components merely because the policy describes those concerns.
