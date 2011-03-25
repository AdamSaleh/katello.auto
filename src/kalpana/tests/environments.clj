(ns kalpana.tests.environments
  (:require [kalpana.tasks :as tasks]
            [kalpana.validation :as validate])
  (:import [org.testng.annotations Test BeforeClass]))

(def test-org-name (atom nil))

(defn ^{BeforeClass {:groups ["setup"]}}
  create_test_org [_]
  (tasks/create-organization (reset! test-org-name (tasks/timestamp "env-test-org")) "organization used to test environments."))

(defn ^{Test {:groups ["environments"]}} create_simple [_]
  (tasks/create-environment @test-org-name
                            (tasks/timestamp "simple-env")
                            "simple environment description"))

(defn ^{Test {:groups ["environments" "blockedByBug-690937"]}} delete_simple [_]
  (let [env-name (tasks/timestamp "delete-env")]
    (tasks/create-environment @test-org-name
                              env-name
                              "simple environment description")
    (tasks/delete-environment @test-org-name env-name)))

(defn ^{Test {:groups ["environments" "validation"]}} name_required [_]
  (validate/name-field-required #(tasks/create-environment @test-org-name nil "env description")))

(defn ^{Test {:groups ["environments" "validation"]}} duplicate_disallowed [_]
  (let [env-name (tasks/timestamp "test-dup")] (validate/duplicate_disallowed #(tasks/create-environment @test-org-name env-name "dup env description"))))
