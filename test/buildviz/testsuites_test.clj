(ns buildviz.testsuites-test
  (:use clojure.test
        buildviz.testsuites))

(deftest TestSuites
  (testing "testsuites-for"
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :fail}]}]
           (testsuites-for "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"><failure/></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :error}]}]
           (testsuites-for "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"><error/></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass}]}]
           (testsuites-for "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a sub suite"
                         :children [{:name "a test"
                                     :status :pass}]}]}]
           (testsuites-for "<testsuites><testsuite name=\"a suite\"><testsuite name=\"a sub suite\"><testcase name=\"a test\"></testcase></testsuite></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass}]}
             {:name "another suite"
              :children [{:name "another test"
                          :status :pass}]}]
           (testsuites-for "<testsuites><testsuite name=\"a suite\"><testcase name=\"a test\"></testcase></testsuite><testsuite name=\"another suite\"><testcase name=\"another test\"></testcase></testsuite></testsuites>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass}]}]
           (testsuites-for "<testsuite name=\"a suite\"><testcase name=\"a test\"></testcase></testsuite>")))
    (is (= [{:name "a suite"
             :children [{:name "a test"
                         :status :pass
                         :runtime 1234}]}]
           (testsuites-for "<testsuite name=\"a suite\"><testcase name=\"a test\" time=\"1.234\"></testcase></testsuite>")))
    ))


(defn- a-testcase [name value]
  (if (contains? #{:pass :fail} value)
    {:name name
     :status value}
    {:name name
     :runtime value}))

(defn- a-testsuite [name & children]
  {:name name
   :children children})

(deftest TestSuiteAccumulation
  (testing "accumulate-testsuite-failures"
    (is (= []
           (accumulate-testsuite-failures [])))
    (is (= []
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :pass))]])))
    (is (= [{:name "suite"
             :children [{:name "a case" :failedCount 1}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :fail))]])))
    (is (= [{:name "suite"
             :children [{:name "a case" :failedCount 1}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :error))]])))
    (is (= [{:name "suite"
             :children [{:name "another case" :failedCount 1}
                        {:name "a case" :failedCount 1}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :fail))]
                                           [(a-testsuite "suite" (a-testcase "another case" :fail))]])))
    (is (= [{:name "another suite"
             :children [{:name "another case" :failedCount 1}]}
            {:name "suite"
             :children [{:name "a case" :failedCount 1}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :fail))]
                                           [(a-testsuite "another suite" (a-testcase "another case" :fail))]])))
    (is (= [{:name "suite"
             :children [{:name "a case" :failedCount 2}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :fail))]
                                           [(a-testsuite "suite" (a-testcase "a case" :fail))]])))
    (is (= [{:name "suite"
             :children [{:name "a case" :failedCount 1}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite" (a-testcase "a case" :pass))]
                                           [(a-testsuite "suite" (a-testcase "a case" :fail))]])))
    (is (= [{:name "suite"
             :children [{:name "nested suite"
                         :children [{:name "another case" :failedCount 1}
                                    {:name "a case" :failedCount 1}]}]}]
           (accumulate-testsuite-failures [[(a-testsuite "suite"
                                                         (a-testsuite "nested suite" (a-testcase "a case" :fail)))]
                                           [(a-testsuite "suite"
                                                         (a-testsuite "nested suite" (a-testcase "another case" :fail)))]])))
    (testing "average-testsuite-duration"
      (is (= []
             (average-testsuite-duration [])))
      (is (= [{:name "suite"
               :children [{:name "a case" :averageRuntime 42}]}]
             (average-testsuite-duration [[(a-testsuite "suite" (a-testcase "a case" 42))]])))
      )))
