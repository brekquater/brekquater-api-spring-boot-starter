// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater
//
// Shows the Brekquater API assessment, either inside Swagger UI or on the standalone page.
// Plain JavaScript on purpose: no build step, no dependencies. Every value from the assessment is inserted as
// text, never as HTML, so nothing in the API document can run as code here.
(function () {
  "use strict";

  var SCRIPT = document.currentScript;
  var BASE = SCRIPT.src.replace(/\/panel\.js(\?.*)?$/, "");
  var RULE_PAGES = "https://starter.brekquater.com/api/rules/";
  var STANDALONE_ROOT_ID = "brekquater-panel";
  var SWAGGER_UI_ANCHOR = ".swagger-ui .information-container";
  var SWAGGER_UI_WAIT_MS = 10000;
  var SEVERITIES = [
    { key: "BLOCKER", label: "Blocker", open: true },
    { key: "MAJOR", label: "Major", open: true },
    { key: "MINOR", label: "Minor", open: false },
    { key: "INFO", label: "Info", open: false }
  ];
  var HTTP_METHODS = ["get", "put", "post", "delete", "options", "head", "patch", "trace"];
  var CATEGORY_NAMES = {
    DESIGN: "Design", DOCUMENTATION: "Documentation", SECURITY: "Security", ERRORS: "Errors",
    NAMING: "Naming", TESTING: "Testing", CODE_QUALITY: "Code quality", ARCHITECTURE: "Architecture"
  };
  var NOT_ASSESSED_REASONS = {
    MISSING_INPUT: "Input not found",
    NOT_IMPLEMENTED: "Not assessed yet"
  };

  function element(tag, className, text) {
    var node = document.createElement(tag);
    if (className) {
      node.className = className;
    }
    if (text !== undefined && text !== null) {
      node.textContent = String(text);
    }
    return node;
  }

  function append(parent, children) {
    children.forEach(function (child) {
      if (child) {
        parent.appendChild(child);
      }
    });
    return parent;
  }

  // ---- Reading the assessment -------------------------------------------------------------------------------

  function load() {
    return fetch(BASE + "/assessment", { headers: { Accept: "application/json" }, credentials: "same-origin" })
      .then(function (response) {
        if (response.status === 401) {
          return { problem: "Sign in to see the API assessment." };
        }
        if (!response.ok) {
          return { problem: "The API assessment could not be loaded (HTTP " + response.status + ")." };
        }
        return response.json().then(function (assessment) {
          return { assessment: assessment };
        });
      })
      .catch(function () {
        return { problem: "The API assessment could not be loaded." };
      });
  }

  // ---- Building the panel -----------------------------------------------------------------------------------

  function panel(outcome, expanded) {
    var root = element("details", "bq-panel");
    root.open = expanded;
    if (outcome.problem) {
      return append(root, [summaryLine(outcome.problem), element("p", "bq-note", outcome.problem)]);
    }
    var assessment = outcome.assessment;
    if (assessment.status !== "ASSESSED") {
      var message = "No OpenAPI document was found, so the API was not assessed.";
      return append(root, [summaryLine(message), element("p", "bq-note", message)]);
    }
    return append(root, [scoreSummary(assessment), body(assessment)]);
  }

  function summaryLine(text) {
    var summary = element("summary", "bq-summary");
    return append(summary, [element("span", "bq-title", "API assessment"), element("span", "bq-muted", text)]);
  }

  function scoreSummary(assessment) {
    var score = assessment.score;
    var summary = element("summary", "bq-summary");
    var facts = element("span", "bq-facts");
    append(facts, [
      element("span", "bq-title", "API maturity"),
      gradeBadge(score.grade),
      securityBadge(assessment.securityRating),
      assessment.blockers > 0 ? element("span", "bq-badge bq-blocker", plural(assessment.blockers, "blocker")) : null,
      element("span", "bq-muted", score.assessedCategories + " of " + score.totalCategories + " categories assessed"),
      element("span", "bq-muted", plural(assessment.findings.length, "finding"))
    ]);
    return append(summary, [ring(score.overall, score.grade), facts]);
  }

  function ring(overall, grade) {
    var wrapper = element("span", "bq-ring bq-grade-" + (grade || "none"));
    var svgNs = "http://www.w3.org/2000/svg";
    var svg = document.createElementNS(svgNs, "svg");
    svg.setAttribute("viewBox", "0 0 36 36");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    var track = document.createElementNS(svgNs, "circle");
    var arc = document.createElementNS(svgNs, "circle");
    [track, arc].forEach(function (circle) {
      circle.setAttribute("cx", "18");
      circle.setAttribute("cy", "18");
      circle.setAttribute("r", "15.5");
      circle.setAttribute("pathLength", "100");
    });
    track.setAttribute("class", "bq-ring-track");
    arc.setAttribute("class", "bq-ring-arc");
    arc.setAttribute("stroke-dasharray", (overall === null ? 0 : overall) + " 100");
    append(svg, [track, arc]);
    var number = element("span", "bq-ring-number", overall === null ? "–" : overall);
    number.setAttribute("aria-hidden", "true");
    var label = overall === null ? "No score" : "Score " + overall + " out of 100, grade " + grade;
    return append(wrapper, [svg, number, element("span", "bq-visually-hidden", label)]);
  }

  function gradeBadge(grade) {
    return grade ? element("span", "bq-badge bq-grade-" + grade, "Grade " + grade) : null;
  }

  function securityBadge(rating) {
    return rating ? element("span", "bq-badge bq-security-" + rating, "Security " + rating) : null;
  }

  function body(assessment) {
    var content = element("div", "bq-body");
    return append(content, [categories(assessment.categories), failedRules(assessment.failedRules),
      findings(assessment.findings)]);
  }

  function categories(list) {
    var section = element("section", "bq-section");
    var tiles = element("ul", "bq-categories");
    list.forEach(function (category) {
      var tile = element("li", "bq-tile" + (category.assessed ? "" : " bq-tile-off"));
      var value = category.assessed
        ? (category.score === null ? "No score" : category.score)
        : NOT_ASSESSED_REASONS[category.reason] || "Not assessed";
      append(tile, [element("span", "bq-tile-value", value),
        element("span", "bq-tile-name", CATEGORY_NAMES[category.name] || category.name)]);
      tiles.appendChild(tile);
    });
    return append(section, [element("h3", "bq-heading", "Categories"), tiles]);
  }

  function failedRules(list) {
    if (!list || list.length === 0) {
      return null;
    }
    var ids = list.map(function (failed) { return failed.ruleId; }).join(", ");
    return element("p", "bq-note", plural(list.length, "rule") + " could not run: " + ids + ".");
  }

  function findings(list) {
    var section = element("section", "bq-section");
    section.appendChild(element("h3", "bq-heading", "Findings"));
    if (list.length === 0) {
      return append(section, [element("p", "bq-note", "No findings. Every rule passed.")]);
    }
    SEVERITIES.forEach(function (severity) {
      var matching = list.filter(function (finding) { return finding.severity === severity.key; });
      if (matching.length > 0) {
        section.appendChild(severityGroup(severity, matching));
      }
    });
    return section;
  }

  function severityGroup(severity, list) {
    var group = element("details", "bq-group");
    group.open = severity.open;
    var summary = element("summary", "bq-group-summary");
    append(summary, [element("span", "bq-chip bq-" + severity.key.toLowerCase(), severity.label),
      element("span", "bq-muted", plural(list.length, "finding"))]);
    var items = element("ul", "bq-findings");
    list.forEach(function (finding) {
      items.appendChild(findingItem(finding));
    });
    return append(group, [summary, items]);
  }

  function findingItem(finding) {
    var item = element("li", "bq-finding");
    var heading = element("p", "bq-finding-heading");
    append(heading, [element("span", "bq-rule-id", finding.ruleId), element("span", "", finding.message)]);
    var where = element("p", "bq-where");
    append(where, [element("span", "", describe(finding.evidence)),
      element("code", "bq-pointer", finding.evidence.location)]);
    return append(item, [heading, where, fix(finding)]);
  }

  function fix(finding) {
    var details = element("details", "bq-fix");
    append(details, [element("summary", "", "How to fix"), element("p", "", finding.remediation)]);
    if (typeof finding.docUrl === "string" && finding.docUrl.indexOf(RULE_PAGES) === 0) {
      var link = element("a", "", "Read about " + finding.ruleId);
      link.href = finding.docUrl;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      details.appendChild(link);
    }
    return details;
  }

  // Turns "/paths/~1orders~1{id}/get/responses/404" into "GET /orders/{id} · responses 404".
  function describe(evidence) {
    if (evidence.source !== "OPENAPI") {
      return evidence.source;
    }
    var tokens = evidence.location.split("/").slice(1).map(function (token) {
      return token.replace(/~1/g, "/").replace(/~0/g, "~");
    });
    if (tokens[0] !== "paths" || tokens.length < 2) {
      return tokens.join(" ") || "Whole document";
    }
    var path = tokens[1];
    if (tokens.length < 3 || HTTP_METHODS.indexOf(tokens[2]) < 0) {
      return path;
    }
    var operation = tokens[2].toUpperCase() + " " + path;
    return tokens.length > 3 ? operation + " · " + tokens.slice(3).join(" ") : operation;
  }

  function plural(count, noun) {
    return count + " " + noun + (count === 1 ? "" : "s");
  }

  // ---- Placing the panel ------------------------------------------------------------------------------------

  function show(outcome) {
    var standaloneRoot = document.getElementById(STANDALONE_ROOT_ID);
    if (standaloneRoot) {
      standaloneRoot.appendChild(panel(outcome, true));
      return;
    }
    whenSwaggerUiIsReady(function (anchor) {
      var wrapper = element("div", "bq-swagger-wrapper");
      wrapper.appendChild(panel(outcome, false));
      anchor.parentNode.insertBefore(wrapper, anchor.nextSibling);
    });
  }

  // Swagger UI renders after the page loads, so wait for its information block. If it never appears, for example
  // because a Swagger UI upgrade changed its markup, show the panel at the top of the page instead.
  function whenSwaggerUiIsReady(place) {
    var found = document.querySelector(SWAGGER_UI_ANCHOR);
    if (found) {
      place(found);
      return;
    }
    var observer = new MutationObserver(function () {
      var anchor = document.querySelector(SWAGGER_UI_ANCHOR);
      if (anchor) {
        observer.disconnect();
        clearTimeout(fallback);
        place(anchor);
      }
    });
    var fallback = setTimeout(function () {
      observer.disconnect();
      var top = element("div", "bq-top-anchor");
      document.body.insertBefore(top, document.body.firstChild);
      place(top);
    }, SWAGGER_UI_WAIT_MS);
    observer.observe(document.body, { childList: true, subtree: true });
  }

  load().then(show);
})();
