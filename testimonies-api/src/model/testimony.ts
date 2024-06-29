import mongoose, { Document, Schema } from 'mongoose';

export interface ITestimony extends Document {
  witnessId: string;
  executionFactId: string;
  timestamp: Date;
}

const testimonySchema = new Schema({
  witnessId: {
    required: true,
    type: String,
  },
  executionFactId: {
    required: true,
    type: String,
  },
  timestamp: {
    required: true,
    type: Date,
  },
});

const Testimony = mongoose.model<ITestimony>("Testimony", testimonySchema);

export default Testimony;
