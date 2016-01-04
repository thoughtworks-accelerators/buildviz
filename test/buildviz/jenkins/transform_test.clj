(ns buildviz.jenkins.transform-test
  (:require [buildviz.jenkins.transform :as sut]
            [clojure.test :refer :all]))

(def a-jenkins-build {:job-name "my_job"
                      :number 42
                      :result "FAILURE"
                      :timestamp 1451877290461
                      :duration 42})

(defn a-jenkins-test-report [testcase-status]
  {:suites [{:name "a test suite"
             :cases [{:className "the class"
                      :name "the test"
                      :duration 0.042
                      :status testcase-status}]}]})

(deftest test-jenkins-build->buildviz-build
  (testing "should map a passed build"
    (is (= {:job-name "my_job"
            :build-id 42
            :build {:outcome "pass"
                    :start 1451877290461
                    :end (+ 1451877290461 42)}
            :test-results nil}
           (sut/jenkins-build->buildviz-build {:job-name "my_job"
                                               :number 42
                                               :result "SUCCESS"
                                               :timestamp 1451877290461
                                               :duration 42}))))

  (testing "should map a failed build"
    (is (= {:outcome "fail"
            :start 1451877290461
            :end (+ 1451877290461 42)}
           (:build (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                          (assoc :result "FAILURE")))))))

  (testing "should extract Git input"
    (is (= [{:source_id "git://some_git"
             :revision "abcd1234"}]
           (:inputs (:build (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                                   (assoc :actions [{}
                                                                                    {:lastBuiltRevision {:SHA1 "abcd1234"}
                                                                                     :remoteUrls ["git://some_git"]}]))))))))

  (testing "should convert test results"
    (is (= [{:name "a test suite"
             :children [{:classname "the class"
                         :name "the test"
                         :runtime 42
                         :status "pass"}]}]
           (:test-results (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                                 (assoc :test-report {:suites [{:name "a test suite"
                                                                                                :cases [{:className "the class"
                                                                                                         :name "the test"
                                                                                                         :duration 0.042
                                                                                                         :status "PASSED"}]}]})))))))
  (testing "should convert test statuses"
    (is (= "pass"
           (-> (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                      (assoc :test-report (a-jenkins-test-report "FIXED"))))
               :test-results
               first
               :children
               first
               :status)))
    (is (= "fail"
           (-> (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                      (assoc :test-report (a-jenkins-test-report "REGRESSION"))))
               :test-results
               first
               :children
               first
               :status)))
    (is (= "fail"
           (-> (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                      (assoc :test-report (a-jenkins-test-report "FAILED"))))
               :test-results
               first
               :children
               first
               :status)))
    (is (= "skipped"
           (-> (sut/jenkins-build->buildviz-build (-> a-jenkins-build
                                                      (assoc :test-report (a-jenkins-test-report "SKIPPED"))))
               :test-results
               first
               :children
               first
               :status)))))