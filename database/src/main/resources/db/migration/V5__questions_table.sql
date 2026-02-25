-- V5: Questions table
-- Holds information for the user Q&A questions.


CREATE TABLE IF NOT EXISTS questions (

    question_id BIGSERIAL PRIMARY KEY,

    question varchar(100) NOT NULL UNIQUE,

    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

)

COMMENT ON TABLE questions IS 'Holds questions for the users Q&A section in their profile';
COMMENT ON TABLE questions.question IS 'A unique question for the user to provide an answer to';